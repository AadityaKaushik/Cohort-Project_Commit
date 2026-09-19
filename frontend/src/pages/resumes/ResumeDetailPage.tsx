import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertTriangleIcon, CopyIcon, DownloadIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import {
  cloneResume,
  deleteResume,
  downloadResumeFile,
  getResume,
  uploadResumeFile,
} from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import { UploadDropzone } from '@/components/shared/UploadDropzone'

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
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={AlertTriangleIcon}
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

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="inline-flex items-center gap-2 text-lg font-semibold">
            {data.name}
            {data.is_default && <Badge variant="outline">Default</Badge>}
          </h2>
          <p className="text-sm text-muted-foreground">{data.version_label ?? 'No version label'}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => cloneMutation.mutate()} disabled={cloneMutation.isPending}>
            <CopyIcon className="h-4 w-4" />
            {cloneMutation.isPending ? 'Cloning…' : 'Clone'}
          </Button>
          <Button variant="outline" size="sm" asChild>
            <Link to={`/resumes/${data.id}/edit`}>
              <PencilIcon className="h-4 w-4" />
              Edit
            </Link>
          </Button>
          <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" size="sm">
                <Trash2Icon className="h-4 w-4" />
                Delete
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Delete this resume?</DialogTitle>
                <DialogDescription>
                  This permanently deletes <strong>{data.name}</strong> and its uploaded file, if any. This cannot be
                  undone.
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
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 text-sm sm:grid-cols-2">
            <Detail label="Target role" value={data.target_role ?? '—'} />
            <Detail label="Created" value={format(new Date(data.created_at), 'MMM d, yyyy')} />
            {data.summary && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Summary</p>
                <p className="whitespace-pre-wrap">{data.summary}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>File</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.has_file ? (
              <div className="space-y-2 text-sm">
                <p className="font-medium">{data.file_original_name}</p>
                <p className="text-muted-foreground">{formatBytes(data.file_size_bytes)}</p>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => downloadMutation.mutate()}
                  disabled={downloadMutation.isPending}
                >
                  <DownloadIcon className="h-4 w-4" />
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
            {uploadError && (
              <p role="alert" className="text-sm text-destructive">
                {uploadError}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-muted-foreground">{label}</p>
      <p>{value}</p>
    </div>
  )
}
