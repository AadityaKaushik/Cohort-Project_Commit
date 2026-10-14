import { memo, useCallback, useState, type DragEvent } from 'react'
import { Link } from 'react-router-dom'
import type { ApplicationStatusValue, ApplicationSummary } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { CompanyMark } from '@/components/shared/CompanyMark'
import { StatusBadge } from '@/components/shared/StatusBadge'

export const BOARD_COLUMNS: ApplicationStatusValue[] = [
  'SAVED',
  'APPLIED',
  'OA',
  'INTERVIEW',
  'FINAL_ROUND',
  'OFFER',
  'ACCEPTED',
  'DECLINED',
  'REJECTED',
  'WITHDRAWN',
]

interface KanbanBoardProps {
  applications: ApplicationSummary[]
  onMove: (application: ApplicationSummary, target: ApplicationStatusValue) => void
}

function canDrop(application: ApplicationSummary | undefined, target: ApplicationStatusValue): boolean {
  if (!application || application.status === target) {
    return false
  }
  return application.allowed_next_statuses.includes(target) || application.correction_statuses.includes(target)
}

// The server's State pattern decides every move: a drop is only previewed as legal from the transitions the API returned, and the final say is still the server's response, so an illegal move can never be persisted.
export function KanbanBoard({ applications, onMove }: KanbanBoardProps) {
  const [draggedId, setDraggedId] = useState<string | null>(null)
  const [overColumn, setOverColumn] = useState<ApplicationStatusValue | null>(null)
  const dragged = applications.find((application) => application.id === draggedId)

  const endDrag = useCallback(() => {
    setDraggedId(null)
    setOverColumn(null)
  }, [])

  function handleDrop(event: DragEvent, target: ApplicationStatusValue) {
    event.preventDefault()
    const application = applications.find((candidate) => candidate.id === (draggedId ?? event.dataTransfer.getData('text/plain')))
    setDraggedId(null)
    setOverColumn(null)
    if (application && application.status !== target) {
      onMove(application, target)
    }
  }

  return (
    <div className="relative -mx-4 flex snap-x gap-3 overflow-x-auto px-4 pb-3 sm:mx-0 sm:px-0" data-testid="kanban-board">
      {BOARD_COLUMNS.map((column) => {
        const cards = applications.filter((application) => application.status === column)
        const droppable = dragged ? canDrop(dragged, column) : false
        return (
          <section
            key={column}
            aria-label={`${formatLabel(column)} column`}
            onDragOver={(event) => {
              if (draggedId) {
                event.preventDefault()
                if (overColumn !== column) {
                  setOverColumn(column)
                }
              }
            }}
            onDragLeave={() => setOverColumn((current) => (current === column ? null : current))}
            onDrop={(event) => handleDrop(event, column)}
            className={cn(
              'flex w-[17rem] shrink-0 snap-start flex-col rounded-xl border bg-surface-2/70 p-2 transition-[background-color,border-color,opacity] duration-150',
              dragged && droppable && 'border-dashed border-primary/50',
              dragged && !droppable && dragged.status !== column && 'opacity-50',
              overColumn === column && droppable && 'bg-primary/8',
            )}
          >
            <h3 className="flex items-center justify-between px-1.5 pt-1 pb-2.5">
              <StatusBadge status={column} />
              <span className="rounded-full bg-card px-2 py-0.5 text-xs font-medium text-muted-foreground tabular-nums ring-1 ring-border">
                {cards.length}
              </span>
            </h3>
            <ul className="flex min-h-20 flex-1 flex-col gap-2">
              {cards.length === 0 && (
                <li className="flex flex-1 items-center justify-center rounded-lg border border-dashed px-3 py-6 text-center text-xs text-muted-foreground">
                  Nothing here yet
                </li>
              )}
              {cards.map((application) => (
                <KanbanCard
                  key={application.id}
                  application={application}
                  isDragged={draggedId === application.id}
                  onDragStart={setDraggedId}
                  onDragEnd={endDrag}
                  onMove={onMove}
                />
              ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}

interface KanbanCardProps {
  application: ApplicationSummary
  isDragged: boolean
  onDragStart: (id: string) => void
  onDragEnd: () => void
  onMove: (application: ApplicationSummary, target: ApplicationStatusValue) => void
}

const KanbanCard = memo(function KanbanCard({ application, isDragged, onDragStart, onDragEnd, onMove }: KanbanCardProps) {
  return (
    <li
      draggable
      onDragStart={(event) => {
        event.dataTransfer.setData('text/plain', application.id)
        event.dataTransfer.effectAllowed = 'move'
        onDragStart(application.id)
      }}
      onDragEnd={onDragEnd}
      className={cn(
        'surface-raised lift cursor-grab rounded-lg border p-3 text-sm hover:border-primary/30 active:cursor-grabbing',
        isDragged && 'opacity-40',
      )}
    >
      <div className="flex items-start gap-2.5">
        <CompanyMark name={application.company.name} className="size-7 text-xs" />
        <div className="min-w-0">
          <Link to={`/applications/${application.id}`} className="block truncate font-medium hover:text-primary">
            {application.company.name}
          </Link>
          <p className="truncate text-xs text-muted-foreground">{application.job_title}</p>
        </div>
        <Link
          to={`/applications/new?from=${application.id}`}
          aria-label={`Duplicate ${application.company.name}, ${application.job_title}`}
          className="ml-auto shrink-0 text-xs text-muted-foreground hover:text-primary"
        >
          Duplicate
        </Link>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 border-t pt-2.5 text-xs text-muted-foreground">
        <span className="tabular-nums">{application.days_in_stage}d in stage</span>
        <label className="sr-only" htmlFor={`move-${application.id}`}>
          Move {application.company.name} to
        </label>
        <select
          id={`move-${application.id}`}
          value=""
          onChange={(event) => {
            if (event.target.value) {
              onMove(application, event.target.value as ApplicationStatusValue)
            }
          }}
          className="h-6 max-w-[7.5rem] rounded-md border bg-card px-1.5 text-xs text-foreground transition-colors hover:border-primary/40"
        >
          <option value="">Move to</option>
          {application.allowed_next_statuses.map((status) => (
            <option key={status} value={status}>
              {formatLabel(status)}
            </option>
          ))}
          {application.correction_statuses.map((status) => (
            <option key={status} value={status}>
              {formatLabel(status)} (correction)
            </option>
          ))}
        </select>
      </div>
    </li>
  )
})
