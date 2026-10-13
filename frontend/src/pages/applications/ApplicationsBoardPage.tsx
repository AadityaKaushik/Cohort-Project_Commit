import { useState } from 'react'
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
import { KanbanBoard } from '@/components/applications/KanbanBoard'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'

interface PendingCorrection {
  application: ApplicationSummary
  target: ApplicationStatusValue
}

export function ApplicationsBoardPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [pending, setPending] = useState<PendingCorrection | null>(null)
  const [note, setNote] = useState('')

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
    if (application.allowed_next_statuses.includes(target)) {
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
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold">Board</h2>
          <p className="text-sm text-muted-foreground">Drag a card to a stage, or use its move menu.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/applications">
              <ListIcon className="h-4 w-4" />
              List
            </Link>
          </Button>
          <Button asChild>
            <Link to="/applications/new">
              <PlusIcon className="h-4 w-4" />
              New application
            </Link>
          </Button>
        </div>
      </div>

      {isLoading && <LoadingRows label="Loading board" />}
      {isError && <ErrorState title="Couldn't load your board" onRetry={() => refetch()} />}
      {data && data.items.length === 0 && (
        <EmptyState
          icon={BriefcaseIcon}
          title="No applications yet"
          description="Add your first application and it will appear in its stage here."
        />
      )}
      {data && data.items.length > 0 && <KanbanBoard applications={data.items} onMove={handleMove} />}

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
