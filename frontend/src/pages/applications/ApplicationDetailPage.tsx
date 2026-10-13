import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertTriangleIcon, CalendarPlusIcon, PencilIcon, Trash2Icon } from 'lucide-react'
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
import { NativeSelect } from '@/components/ui/native-select'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Timeline } from '@/components/shared/Timeline'

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

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">{data.job_title}</h2>
          <p className="text-sm text-muted-foreground">{data.company.name}</p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={data.status} />
          <Button variant="outline" size="sm" asChild>
            <Link to={`/applications/${data.id}/edit`}>
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
                <DialogTitle>Delete this application?</DialogTitle>
                <DialogDescription>
                  This permanently deletes the application for <strong>{data.company.name}</strong> ({data.job_title}
                  ) and its full history. This cannot be undone.
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
            <Detail label="Date applied" value={format(new Date(data.date_applied), 'MMM d, yyyy')} />
            <Detail label="Current stage" value={formatLabel(data.current_stage)} />
            <Detail label="Days in stage" value={String(data.days_in_stage)} />
            <Detail label="Priority" value={formatLabel(data.priority)} />
            <Detail label="Location" value={data.location ?? '—'} />
            <Detail label="Job type" value={formatLabel(data.job_type)} />
            <Detail
              label="Salary"
              value={
                data.salary_min !== null || data.salary_max !== null
                  ? `${data.salary_min ?? '?'} to ${data.salary_max ?? '?'} ${data.salary_currency ?? ''}`.trim()
                  : '—'
              }
            />
            <Detail label="Source" value={data.source ?? '—'} />
            <Detail label="Referral" value={data.is_referral ? 'Yes' : 'No'} />
            <Detail label="Deadline" value={data.deadline ? format(new Date(data.deadline), 'MMM d, yyyy') : '—'} />
            <Detail label="Recruiter" value={[data.recruiter_name, data.recruiter_email].filter(Boolean).join(' · ') || '—'} />
            <Detail label="Work mode" value={formatLabel(data.work_mode)} />
            {data.job_url && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Job posting</p>
                <a
                  href={data.job_url}
                  target="_blank"
                  rel="noreferrer"
                  className="text-primary underline-offset-4 hover:underline"
                >
                  {data.job_url}
                </a>
              </div>
            )}
            {data.job_description && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Job description</p>
                <p className="whitespace-pre-wrap">{data.job_description}</p>
              </div>
            )}
            {data.notes && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Notes</p>
                <p className="whitespace-pre-wrap">{data.notes}</p>
              </div>
            )}
            {data.cover_letter && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Cover letter</p>
                <p className="whitespace-pre-wrap">{data.cover_letter}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Change status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.allowed_next_statuses.length === 0 && data.correction_statuses.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                This application is closed, so its status can no longer change.
              </p>
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
            {statusError && (
              <p role="alert" className="text-sm text-destructive">
                {statusError}
              </p>
            )}
            <Button
              className="w-full"
              disabled={!nextStatus || statusMutation.isPending || (needsNote && note.trim() === '')}
              onClick={() => statusMutation.mutate()}
            >
              {statusMutation.isPending ? 'Updating…' : 'Update status'}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Resume</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Select
              value={data.resume_id ?? 'none'}
              onValueChange={(value) => attachResumeMutation.mutate(value === 'none' ? null : value)}
              disabled={attachResumeMutation.isPending || resumesQuery.isLoading}
            >
              <SelectTrigger>
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
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle>Interviews</CardTitle>
          <Button variant="outline" size="sm" asChild>
            <Link to={`/interviews/new?application_id=${data.id}`}>
              <CalendarPlusIcon className="h-4 w-4" />
              Add interview
            </Link>
          </Button>
        </CardHeader>
        <CardContent>
          {data.interviews.length === 0 ? (
            <p className="text-sm text-muted-foreground">No interviews recorded for this application yet.</p>
          ) : (
            <ul className="space-y-2">
              {data.interviews.map((interview) => (
                <li key={interview.id}>
                  <Link
                    to={`/interviews/${interview.id}`}
                    className="flex items-center justify-between rounded-md border p-3 text-sm hover:bg-accent"
                  >
                    <span>
                      Round {interview.round_number} — {formatLabel(interview.round_type)}
                    </span>
                    <Badge variant="outline">{formatLabel(interview.outcome)}</Badge>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Timeline</CardTitle>
        </CardHeader>
        <CardContent>
          <Timeline entries={timelineEntries} />
        </CardContent>
      </Card>
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
