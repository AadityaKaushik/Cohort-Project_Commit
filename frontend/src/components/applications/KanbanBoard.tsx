import { memo, useCallback, useState, type DragEvent } from 'react'
import { Link } from 'react-router-dom'
import type { ApplicationStatusValue, ApplicationSummary } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import { cn } from '@/lib/utils'
import { LINE_BORDER_LEFT, LINE_BORDER_TOP, STATUS_LINE } from '@/lib/stage-lines'

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
              'flex w-[17rem] shrink-0 snap-start flex-col rounded-md border border-t-[6px] bg-surface-2 p-2 transition-[background-color,border-color,opacity] duration-150',
              LINE_BORDER_TOP[STATUS_LINE[column]],
              dragged && droppable && 'border-dashed border-x-foreground/50 border-b-foreground/50',
              dragged && !droppable && dragged.status !== column && 'opacity-40',
              overColumn === column && droppable && 'bg-accent',
            )}
          >
            <h3 className="flex items-baseline justify-between px-1.5 pt-1.5 pb-3">
              <span className="text-display text-xl">{formatLabel(column)}</span>
              <span className="tabular text-sm font-semibold">{cards.length}</span>
            </h3>
            <ul className="flex min-h-20 flex-1 flex-col gap-2">
              {cards.length === 0 && (
                <li className="board-label flex flex-1 items-center justify-center rounded-[3px] border border-dashed px-3 py-6 text-center text-muted-foreground">
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
        'cursor-grab rounded-[3px] border border-l-4 bg-card p-3 text-sm transition-colors hover:border-foreground/40 active:cursor-grabbing',
        LINE_BORDER_LEFT[STATUS_LINE[application.status]],
        isDragged && 'opacity-40',
      )}
    >
      <div className="flex items-start gap-2.5">
        <div className="min-w-0">
          <Link
            to={`/applications/${application.id}`}
            className="block truncate text-[15px] font-semibold hover:underline hover:decoration-signal hover:decoration-2 hover:underline-offset-2"
          >
            {application.company.name}
          </Link>
          <p className="truncate text-xs text-muted-foreground">{application.job_title}</p>
        </div>
        <Link
          to={`/applications/new?from=${application.id}`}
          aria-label={`Duplicate ${application.company.name}, ${application.job_title}`}
          className="board-label ml-auto shrink-0 pt-1 text-[10px] text-muted-foreground hover:text-foreground"
        >
          Duplicate
        </Link>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 border-t pt-2.5 text-xs text-muted-foreground">
        <span className="tabular">{application.days_in_stage}d in stage</span>
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
          className="h-6 max-w-[7.5rem] rounded-[3px] border bg-card px-1.5 text-xs text-foreground transition-colors hover:border-foreground"
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
