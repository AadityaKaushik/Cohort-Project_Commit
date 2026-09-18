import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertTriangleIcon, PencilIcon, Trash2Icon } from 'lucide-react'
import { changeApplicationStatus, deleteApplication, getApplication, type ApplicationStatusValue } from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Timeline } from '@/components/shared/Timeline'

const ALL_STATUSES: ApplicationStatusValue[] = [
  'SAVED',
  'APPLIED',
  'OA',
  'INTERVIEW',
  'FINAL_ROUND',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
  'ACCEPTED',
  'DECLINED',
]

export function ApplicationDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['application', id],
    queryFn: () => getApplication(id!),
    enabled: Boolean(id),
  })

  const [nextStatus, setNextStatus] = useState<ApplicationStatusValue | undefined>(undefined)
  const [note, setNote] = useState('')
  const [statusError, setStatusError] = useState<string | null>(null)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const statusMutation = useMutation({
    mutationFn: () =>
      changeApplicationStatus(id!, { to_status: nextStatus as ApplicationStatusValue, note: note || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application', id] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      setNextStatus(undefined)
      setNote('')
      setStatusError(null)
    },
    onError: (mutationError: unknown) => {
      setStatusError(mutationError instanceof ApiError ? mutationError.message : 'Could not change status.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteApplication(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      navigate('/applications')
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

  const timelineEntries = data.history.map((entry) => ({
    id: entry.id,
    title: entry.from_status ? `${entry.from_status} → ${entry.to_status}` : `Created as ${entry.to_status}`,
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
            <Detail label="Current stage" value={data.current_stage} />
            <Detail label="Days in stage" value={String(data.days_in_stage)} />
            <Detail label="Priority" value={data.priority ?? '—'} />
            <Detail label="Location" value={data.location ?? '—'} />
            <Detail label="Work mode" value={data.work_mode ?? '—'} />
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
            {data.notes && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Notes</p>
                <p className="whitespace-pre-wrap">{data.notes}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Change status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Select value={nextStatus} onValueChange={(value) => setNextStatus(value as ApplicationStatusValue)}>
              <SelectTrigger>
                <SelectValue placeholder="Choose a status" />
              </SelectTrigger>
              <SelectContent>
                {ALL_STATUSES.filter((option) => option !== data.status).map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Textarea
              placeholder="Optional note (required to move backward as a correction)"
              value={note}
              onChange={(event) => setNote(event.target.value)}
              rows={2}
            />
            {statusError && (
              <p role="alert" className="text-sm text-destructive">
                {statusError}
              </p>
            )}
            <Button className="w-full" disabled={!nextStatus || statusMutation.isPending} onClick={() => statusMutation.mutate()}>
              {statusMutation.isPending ? 'Updating…' : 'Update status'}
            </Button>
          </CardContent>
        </Card>
      </div>

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
