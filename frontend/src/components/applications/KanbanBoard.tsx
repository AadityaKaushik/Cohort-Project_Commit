import { useState, type DragEvent } from 'react'
import { Link } from 'react-router-dom'
import type { ApplicationStatusValue, ApplicationSummary } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import { cn } from '@/lib/utils'

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

/**
 * The server's State pattern decides every move: a drop is only previewed as
 * legal from the transitions the API returned, and the final say is still the
 * server's response, so an illegal move can never be persisted.
 */
export function KanbanBoard({ applications, onMove }: KanbanBoardProps) {
  const [draggedId, setDraggedId] = useState<string | null>(null)
  const [overColumn, setOverColumn] = useState<ApplicationStatusValue | null>(null)
  const dragged = applications.find((application) => application.id === draggedId)

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
    <div className="flex gap-3 overflow-x-auto pb-2" data-testid="kanban-board">
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
                setOverColumn(column)
              }
            }}
            onDragLeave={() => setOverColumn((current) => (current === column ? null : current))}
            onDrop={(event) => handleDrop(event, column)}
            className={cn(
              'flex w-64 shrink-0 flex-col rounded-lg border bg-muted/30 p-2 transition-colors',
              dragged && droppable && 'border-primary/50',
              dragged && !droppable && dragged.status !== column && 'opacity-60',
              overColumn === column && droppable && 'bg-primary/5',
            )}
          >
            <h3 className="flex items-center justify-between px-1 pb-2 text-sm font-medium">
              {formatLabel(column)}
              <span className="text-xs font-normal text-muted-foreground">{cards.length}</span>
            </h3>
            <ul className="flex min-h-16 flex-1 flex-col gap-2">
              {cards.map((application) => (
                <li
                  key={application.id}
                  draggable
                  onDragStart={(event) => {
                    event.dataTransfer.setData('text/plain', application.id)
                    event.dataTransfer.effectAllowed = 'move'
                    setDraggedId(application.id)
                  }}
                  onDragEnd={() => {
                    setDraggedId(null)
                    setOverColumn(null)
                  }}
                  className={cn(
                    'cursor-grab rounded-md border bg-card p-2.5 text-sm active:cursor-grabbing',
                    draggedId === application.id && 'opacity-40',
                  )}
                >
                  <Link to={`/applications/${application.id}`} className="font-medium hover:underline">
                    {application.company.name}
                  </Link>
                  <p className="text-muted-foreground">{application.job_title}</p>
                  <div className="mt-2 flex items-center justify-between gap-2 text-xs text-muted-foreground">
                    <span>{application.days_in_stage} days</span>
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
                      className="h-6 max-w-[7.5rem] rounded border bg-background px-1 text-xs"
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
              ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}
