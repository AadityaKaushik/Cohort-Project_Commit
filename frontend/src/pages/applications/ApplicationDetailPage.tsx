import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { CalendarPlusIcon, PencilIcon, SearchXIcon, Trash2Icon } from 'lucide-react'
import {
  attachResumeToApplication,
  changeApplicationStatus,
  deleteApplication,
  getApplication,
  type ApplicationStatusValue,
} from '@/lib/applications-api'
import { listResumes } from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { useToast } from '@/hooks/use-toast'
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
import { NativeSelect } from '@/components/ui/native-select'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Timeline } from '@/components/shared/Timeline'
import { DetailList, type DetailItem } from '@/components/shared/DetailList'
import { FormError } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { OutcomeBadge, PriorityBadge } from '@/components/shared/ToneBadge'

export function ApplicationDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['application', id],
    queryFn: () => getApplication(id!),
    enabled: Boolean(id),
  })

  const [nextStatus, setNextStatus] = useState<ApplicationStatusValue | ''>('')
  const [note, setNote] = useState('')
  const [statusError, setStatusError] = useState<string | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const statusMutation = useMutation({
    mutationFn: () =>
      changeApplicationStatus(id!, { to_status: nextStatus as ApplicationStatusValue, note: note || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application', id] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      setNextStatus('')
      setNote('')
      setStatusError(null)
      toast({ title: 'Status updated' })
    },
    onError: (mutationError: unknown) => {
      setStatusError(mutationError instanceof ApiError ? mutationError.message : 'Could not change status.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteApplication(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      toast({ title: 'Application deleted' })
      navigate('/applications')
    },
    onError: () => {
      toast({ title: 'Could not delete the application', variant: 'error' })
    },
  })

  const resumesQuery = useQuery({
    queryKey: ['resumes', 'for-attach'],
    queryFn: () => listResumes({ size: 100 }),
  })

  const attachResumeMutation = useMutation({
    mutationFn: (resumeId: string | null) => attachResumeToApplication(id!, resumeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application', id] })
      toast({ title: 'Resume updated' })
    },
    onError: () => {
      toast({ title: 'Could not update the resume', variant: 'error' })
    },
  })

  if (isLoading) {
    return <PageSkeleton label="Loading application" />
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={SearchXIcon}
        title="Application not found"
        description="It may have been deleted, or the link may be wrong."
        action={
          <Button asChild>
            <Link to="/applications">Back to applications</Link>
          </Button>
        }
      />
    )
  }

  const needsNote = nextStatus !== '' && (data.correction_statuses as string[]).includes(nextStatus)

  const timelineEntries = data.history.map((entry) => ({
    id: entry.id,
    title: entry.from_status
      ? `${formatLabel(entry.from_status)} → ${formatLabel(entry.to_status)}`
      : `Created as ${formatLabel(entry.to_status)}`,
    description: entry.note,
    timestamp: entry.changed_at,
  }))

  const salary =
    data.salary_min !== null || data.salary_max !== null
      ? `${data.salary_min ?? '?'} to ${data.salary_max ?? '?'} ${data.salary_currency ?? ''}`.trim()
      : '—'
  const details: DetailItem[] = [
    { label: 'Date applied', value: format(new Date(data.date_applied), 'MMM d, yyyy') },
    { label: 'Current stage', value: formatLabel(data.current_stage) },
    { label: 'Days in stage', value: String(data.days_in_stage) },
    { label: 'Priority', value: <PriorityBadge priority={data.priority} /> },
    { label: 'Location', value: data.location ?? '—' },
    { label: 'Work mode', value: formatLabel(data.work_mode) },
    { label: 'Job type', value: formatLabel(data.job_type) },
    { label: 'Salary', value: salary },
    { label: 'Source', value: data.source ?? '—' },
    { label: 'Referral', value: data.is_referral ? 'Yes' : 'No' },
    { label: 'Deadline', value: data.deadline ? format(new Date(data.deadline), 'MMM d, yyyy') : '—' },
    { label: 'Recruiter', value: [data.recruiter_name, data.recruiter_email].filter(Boolean).join(' · ') || '—' },
  ]
  if (data.job_url) {
    details.push({
      label: 'Job posting',
      wide: true,
      value: (
        <a href={data.job_url} target="_blank" rel="noreferrer" className="break-all text-primary underline-offset-4 hover:underline">
          {data.job_url}
        </a>
      ),
    })
  }
  if (data.job_description) details.push({ label: 'Job description', value: data.job_description, wide: true })
  if (data.notes) details.push({ label: 'Notes', value: data.notes, wide: true })
  if (data.cover_letter) details.push({ label: 'Cover letter', value: data.cover_letter, wide: true })

  return (
    <div className="space-y-6">
      <PageHeader
        back={{ to: '/applications', label: 'Applications' }}
        title={data.job_title}
        description={data.company.name}
        meta={<StatusBadge status={data.status} />}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to={`/applications/${data.id}/edit`}>
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
                  <DialogTitle>Delete this application?</DialogTitle>
                  <DialogDescription>
                    This permanently deletes the application for <strong>{data.company.name}</strong> ({data.job_title}) and its full
                    history. This cannot be undone.
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

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="stagger min-w-0 space-y-6 lg:col-span-2">
          <SectionCard title="Details">
            <DetailList items={details} />
          </SectionCard>

          <SectionCard
            title="Interviews"
            action={
              <Button variant="outline" size="sm" asChild>
                <Link to={`/interviews/new?application_id=${data.id}`}>
                  <CalendarPlusIcon aria-hidden />
                  Add interview
                </Link>
              </Button>
            }
          >
            {data.interviews.length === 0 ? (
              <p className="rounded-lg border border-dashed bg-surface-2/50 p-5 text-center text-sm text-muted-foreground">
                No interviews recorded for this application yet.
              </p>
            ) : (
              <ul className="space-y-2">
                {data.interviews.map((interview) => (
                  <li key={interview.id}>
                    <Link
                      to={`/interviews/${interview.id}`}
                      className="lift flex items-center justify-between gap-3 rounded-lg border bg-card p-3 text-sm hover:border-primary/30"
                    >
                      <span className="font-medium">
                        Round {interview.round_number} · {formatLabel(interview.round_type)}
                      </span>
                      <OutcomeBadge outcome={interview.outcome} />
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>

          <SectionCard title="Timeline" description="Every status change, newest first.">
            <Timeline entries={timelineEntries} />
          </SectionCard>
        </div>

        <div className="stagger min-w-0 space-y-6">
          <SectionCard title="Change status" description="Only moves the pipeline allows are offered.">
            <div className="space-y-3">
              {data.allowed_next_statuses.length === 0 && data.correction_statuses.length === 0 ? (
                <p className="text-sm text-muted-foreground">This application is closed, so its status can no longer change.</p>
              ) : (
                <>
                  <label htmlFor="next-status" className="sr-only">
                    New status
                  </label>
                  <NativeSelect
                    id="next-status"
                    value={nextStatus}
                    onChange={(event) => {
                      setNextStatus(event.target.value as ApplicationStatusValue | '')
                      setStatusError(null)
                    }}
                  >
                    <option value="">Choose a status</option>
                    {data.allowed_next_statuses.map((option) => (
                      <option key={option} value={option}>
                        {formatLabel(option)}
                      </option>
                    ))}
                    {data.correction_statuses.length > 0 && (
                      <optgroup label="Move back (note required)">
                        {data.correction_statuses.map((option) => (
                          <option key={option} value={option}>
                            {formatLabel(option)}
                          </option>
                        ))}
                      </optgroup>
                    )}
                  </NativeSelect>
                  <Textarea
                    aria-label="Note"
                    placeholder={needsNote ? 'Why are you moving this back? (required)' : 'Optional note'}
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    rows={2}
                  />
                </>
              )}
              {statusError && <FormError>{statusError}</FormError>}
              <Button
                className="w-full"
                disabled={!nextStatus || statusMutation.isPending || (needsNote && note.trim() === '')}
                onClick={() => statusMutation.mutate()}
              >
                {statusMutation.isPending ? 'Updating…' : 'Update status'}
              </Button>
            </div>
          </SectionCard>

          <SectionCard title="Resume" description="The version you sent with this application.">
            <div className="space-y-3">
              <Select
                value={data.resume_id ?? 'none'}
                onValueChange={(value) => attachResumeMutation.mutate(value === 'none' ? null : value)}
                disabled={attachResumeMutation.isPending || resumesQuery.isLoading}
              >
                <SelectTrigger aria-label="Attached resume" className="w-full">
                  <SelectValue placeholder="No resume attached" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">No resume attached</SelectItem>
                  {resumesQuery.data?.items.map((resume) => (
                    <SelectItem key={resume.id} value={resume.id}>
                      {resume.name}
                      {resume.version_label ? ` (${resume.version_label})` : ''}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {data.resume_id && (
                <Button variant="outline" size="sm" className="w-full" asChild>
                  <Link to={`/resumes/${data.resume_id}`}>View resume</Link>
                </Button>
              )}
            </div>
          </SectionCard>
        </div>
      </div>
    </div>
  )
}
