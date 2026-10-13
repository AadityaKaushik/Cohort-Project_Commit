import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { CopyIcon, DownloadIcon, FileTextIcon, PencilIcon, SearchXIcon, Trash2Icon } from 'lucide-react'
import {
  cloneResume,
  deleteResume,
  downloadResumeFile,
  getResume,
  uploadResumeFile,
} from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { EmptyState } from '@/components/shared/EmptyState'
import { UploadDropzone } from '@/components/shared/UploadDropzone'
import { DetailList, type DetailItem } from '@/components/shared/DetailList'
import { FormError } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { ToneBadge } from '@/components/shared/ToneBadge'

function formatBytes(bytes: number | null): string {
  if (bytes === null) return '—'
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024).toFixed(1)} KB`
}

export function ResumeDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['resume', id],
    queryFn: () => getResume(id!),
    enabled: Boolean(id),
  })

  const [deleteOpen, setDeleteOpen] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadResumeFile(id!, file),
    onSuccess: () => {
      setUploadError(null)
      queryClient.invalidateQueries({ queryKey: ['resume', id] })
      queryClient.invalidateQueries({ queryKey: ['resumes'] })
    },
    onError: (mutationError: unknown) => {
      setUploadError(mutationError instanceof ApiError ? mutationError.message : 'Upload failed. Please try again.')
    },
  })

  const downloadMutation = useMutation({
    mutationFn: () => downloadResumeFile(id!),
    onSuccess: ({ blob, filename }) => {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename ?? 'resume'
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    },
  })

  const cloneMutation = useMutation({
    mutationFn: () => cloneResume(id!),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] })
      navigate(`/resumes/${result.id}`)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteResume(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] })
      navigate('/resumes')
    },
  })

  if (isLoading) {
    return <PageSkeleton label="Loading resume" />
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={SearchXIcon}
        title="Resume not found"
        description="It may have been deleted, or the link may be wrong."
        action={
          <Button asChild>
            <Link to="/resumes">Back to resumes</Link>
          </Button>
        }
      />
    )
  }

  const details: DetailItem[] = [
    { label: 'Version', value: data.version_label ?? '—' },
    { label: 'Target role', value: data.target_role ?? '—' },
    { label: 'Created', value: format(new Date(data.created_at), 'MMM d, yyyy') },
    { label: 'Default', value: data.is_default ? 'Yes, attached to new applications first' : 'No' },
  ]
  if (data.summary) details.push({ label: 'Summary', value: data.summary, wide: true })

  return (
    <div className="space-y-6">
      <PageHeader
        back={{ to: '/resumes', label: 'Resumes' }}
        title={data.name}
        description={data.version_label ? `Version ${data.version_label}` : 'No version label'}
        meta={data.is_default ? <ToneBadge tone="primary">Default</ToneBadge> : undefined}
        actions={
          <>
            <Button variant="outline" onClick={() => cloneMutation.mutate()} disabled={cloneMutation.isPending}>
              <CopyIcon aria-hidden />
              {cloneMutation.isPending ? 'Cloning…' : 'Clone'}
            </Button>
            <Button variant="outline" asChild>
              <Link to={`/resumes/${data.id}/edit`}>
                <PencilIcon aria-hidden />
                Edit
              </Link>
            </Button>
            <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
              <DialogTrigger asChild>
                <Button variant="destructive">
                  <Trash2Icon aria-hidden />
                  Delete
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Delete this resume?</DialogTitle>
                  <DialogDescription>
                    This permanently deletes <strong>{data.name}</strong> and its uploaded file, if any. This cannot be undone.
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setDeleteOpen(false)}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={() => deleteMutation.mutate()} disabled={deleteMutation.isPending}>
                    {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        }
      />

      <div className="stagger grid gap-6 lg:grid-cols-3">
        <SectionCard title="Details" className="lg:col-span-2">
          <DetailList items={details} />
        </SectionCard>

        <SectionCard title="File" description={data.has_file ? 'The document you send out.' : 'Upload the document you send out.'}>
          <div className="space-y-4">
            {data.has_file ? (
              <div className="flex items-center gap-3 rounded-lg border bg-surface-2/50 p-3">
                <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-info/10 text-info">
                  <FileTextIcon className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{data.file_original_name}</p>
                  <p className="text-xs text-muted-foreground">{formatBytes(data.file_size_bytes)}</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => downloadMutation.mutate()} disabled={downloadMutation.isPending}>
                  <DownloadIcon aria-hidden />
                  {downloadMutation.isPending ? 'Downloading…' : 'Download'}
                </Button>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">No file uploaded yet.</p>
            )}
            <UploadDropzone
              onFileSelected={(file) => uploadMutation.mutate(file)}
              isUploading={uploadMutation.isPending}
              helperText={data.has_file ? 'Uploading a new file replaces the current one' : 'PDF or Word document, up to 5 MB'}
            />
            {uploadError && <FormError>{uploadError}</FormError>}
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
