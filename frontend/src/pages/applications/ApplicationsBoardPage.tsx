import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BriefcaseIcon, ListIcon, PlusIcon } from 'lucide-react'
import {
  changeApplicationStatus,
  listApplications,
  type ApplicationStatusValue,
  type ApplicationSummary,
} from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { usePendingDeletes } from '@/hooks/use-undo-delete'
import { writePreference } from '@/lib/preferences'
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
} from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { InterviewMoveDialog } from '@/components/applications/InterviewMoveDialog'
import { KanbanBoard } from '@/components/applications/KanbanBoard'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/QueryStates'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { Skeleton } from '@/components/ui/skeleton'

interface PendingCorrection {
  application: ApplicationSummary
  target: ApplicationStatusValue
}

export function ApplicationsBoardPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const pendingDeletes = usePendingDeletes()
  const [pending, setPending] = useState<PendingCorrection | null>(null)
  const [note, setNote] = useState('')
  const [interviewFor, setInterviewFor] = useState<ApplicationSummary | null>(null)

  useEffect(() => {
    writePreference('applications-view', 'board')
  }, [])

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['applications', 'board'],
    queryFn: () => listApplications({ size: 500, sort: 'date_applied', direction: 'desc' }),
  })

  const moveMutation = useMutation({
    mutationFn: (input: { id: string; to_status: ApplicationStatusValue; note?: string }) =>
      changeApplicationStatus(input.id, { to_status: input.to_status, note: input.note }),
    onSuccess: (result, input) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['application', input.id] })
      toast({ title: `Moved to ${formatLabel(result.status)}` })
    },
    onError: (error: unknown) => {
      toast({
        title: 'Move not allowed',
        description: error instanceof ApiError ? error.message : 'Something went wrong. The card stayed where it was.',
        variant: 'error',
      })
    },
  })

  function handleMove(application: ApplicationSummary, target: ApplicationStatusValue) {
    if (target === 'INTERVIEW' && application.allowed_next_statuses.includes(target)) {
      setInterviewFor(application)
    } else if (application.allowed_next_statuses.includes(target)) {
      moveMutation.mutate({ id: application.id, to_status: target })
    } else if (application.correction_statuses.includes(target)) {
      setNote('')
      setPending({ application, target })
    } else {
      toast({
        title: 'Move not allowed',
        description: `An application cannot move from ${formatLabel(application.status)} to ${formatLabel(target)}.`,
        variant: 'error',
      })
    }
  }

  function confirmCorrection() {
    if (!pending || note.trim() === '') {
      return
    }
    moveMutation.mutate({ id: pending.application.id, to_status: pending.target, note: note.trim() })
    setPending(null)
  }

  return (
    <div className="space-y-5">
      <PageHeader
        title="Application board"
        description="Drag a card to a stage, or use its move menu. Moves the pipeline does not allow are blocked."
        actions={
          <>
            <PageGuide guideId="applications" />
            <Button variant="outline" asChild>
              <Link to="/applications?view=list">
                <ListIcon aria-hidden />
                List
              </Link>
            </Button>
            <Button asChild>
              <Link to="/applications/new">
                <PlusIcon aria-hidden />
                New application
              </Link>
            </Button>
          </>
        }
      />

      {isLoading && <BoardSkeleton />}
      {isError && <ErrorState title="Couldn't load your board" onRetry={() => refetch()} />}
      {data && data.items.length === 0 && (
        <EmptyState
          icon={BriefcaseIcon}
          title="No applications yet"
          description="Add your first application and it will appear in its stage here."
          action={
            <Button asChild>
              <Link to="/applications/new">
                <PlusIcon aria-hidden />
                Add an application
              </Link>
            </Button>
          }
        />
      )}
      {data && data.items.length > 0 && (
        <div data-guide="board">
          <KanbanBoard applications={data.items.filter((item) => !pendingDeletes.has(item.id))} onMove={handleMove} />
        </div>
      )}

      {interviewFor && (
        <InterviewMoveDialog
          applicationId={interviewFor.id}
          companyName={interviewFor.company.name}
          onClose={() => setInterviewFor(null)}
        />
      )}

      <Dialog open={pending !== null} onOpenChange={(open) => !open && setPending(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Move back to {pending ? formatLabel(pending.target) : ''}?</DialogTitle>
            <DialogDescription>
              Moving backwards is recorded as a correction. Add a short note so your history stays accurate.
            </DialogDescription>
          </DialogHeader>
          <Textarea
            aria-label="Correction note"
            placeholder="Why are you moving this back?"
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={3}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setPending(null)}>
              Cancel
            </Button>
            <Button disabled={note.trim() === ''} onClick={confirmCorrection}>
              Move back
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function BoardSkeleton() {
  return (
    <div role="status" aria-label="Loading board" className="flex gap-3 overflow-hidden">
      {Array.from({ length: 5 }).map((_, column) => (
        <div key={column} className="w-[17rem] shrink-0 space-y-2 rounded-xl border bg-surface-2/70 p-2">
          <Skeleton className="m-1.5 h-5 w-20 rounded-full" />
          {Array.from({ length: 3 - (column % 2) }).map((_, card) => (
            <Skeleton key={card} className="h-24 w-full rounded-lg" />
          ))}
        </div>
      ))}
    </div>
  )
}
