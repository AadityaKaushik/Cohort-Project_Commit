import { useState, type DragEvent } from 'react'
import { ArrowRightIcon, CheckIcon, OctagonAlertIcon } from 'lucide-react'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { DEMO_COLUMNS as COLUMNS, INITIAL_DEMO_CARDS as INITIAL_CARDS, moveVerdict, type MoveFeedback as Feedback } from '@/lib/demo-board'
import { cn } from '@/lib/utils'

export function InteractiveBoard() {
  const [cards, setCards] = useState(INITIAL_CARDS)
  const [dragging, setDragging] = useState<string | null>(null)
  const [hover, setHover] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback>({ tone: 'ok', message: 'Drag a card to the next stage, or use its arrow.' })
  const [rejected, setRejected] = useState<string | null>(null)

  const draggedCard = cards.find((card) => card.id === dragging)

  function tryMove(id: string, to: number) {
    const card = cards.find((item) => item.id === id)
    if (!card) return
    const verdict = moveVerdict(card.column, to)
    if (!verdict) return
    setFeedback({ tone: verdict.tone, message: verdict.tone === 'ok' ? `${card.company}: ${verdict.message}` : verdict.message })
    if (verdict.tone === 'ok') {
      setCards((current) => current.map((item) => (item.id === id ? { ...item, column: to } : item)))
    } else {
      setRejected(id)
      window.setTimeout(() => setRejected(null), 450)
    }
  }

  function handleDrop(event: DragEvent, column: number) {
    event.preventDefault()
    const id = event.dataTransfer.getData('text/plain') || dragging
    setHover(null)
    setDragging(null)
    if (id) tryMove(id, column)
  }

  function resetDemo() {
    setCards(INITIAL_CARDS)
    setFeedback({ tone: 'ok', message: 'Board reset. Drag a card to the next stage.' })
  }

  return (
    <div className="flex h-full flex-col gap-3">
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        {COLUMNS.map((column, columnIndex) => {
          const columnCards = cards.filter((card) => card.column === columnIndex)
          const verdict = draggedCard ? moveVerdict(draggedCard.column, columnIndex) : null
          return (
            <div
              key={column.status}
              onDragOver={(event) => {
                event.preventDefault()
                setHover(columnIndex)
              }}
              onDragLeave={() => setHover((current) => (current === columnIndex ? null : current))}
              onDrop={(event) => handleDrop(event, columnIndex)}
              data-target={hover === columnIndex && verdict ? verdict.tone : undefined}
              className="min-h-28 min-w-0 space-y-2 rounded-lg border border-transparent p-1.5 transition-colors duration-150 data-[target=blocked]:border-warning/60 data-[target=blocked]:bg-warning-soft/60 data-[target=ok]:border-success/60 data-[target=ok]:bg-success/8"
            >
              <p className="flex items-center justify-between px-0.5 text-[11px] font-medium text-muted-foreground">
                <span className="truncate">{column.label}</span>
                <span className="tabular-nums">{columnCards.length}</span>
              </p>
              {columnCards.map((card) => (
                <div
                  key={card.id}
                  draggable
                  onDragStart={(event) => {
                    event.dataTransfer.setData('text/plain', card.id)
                    event.dataTransfer.effectAllowed = 'move'
                    setDragging(card.id)
                  }}
                  onDragEnd={() => {
                    setDragging(null)
                    setHover(null)
                  }}
                  className={cn(
                    'group/card animate-enter cursor-grab rounded-lg border bg-card p-2 transition-[transform,box-shadow,opacity] duration-150 hover:-translate-y-0.5 hover:shadow-md active:cursor-grabbing',
                    dragging === card.id && 'opacity-50',
                    rejected === card.id && 'animate-shake',
                  )}
                >
                  <p className="truncate text-xs font-medium">{card.company}</p>
                  <p className="truncate text-[11px] text-muted-foreground">{card.role}</p>
                  <div className="mt-1.5 flex items-center justify-between gap-1">
                    <StatusBadge status={column.status} className="h-4 px-1.5 text-[10px]" />
                    {card.column < COLUMNS.length - 1 && (
                      <button
                        type="button"
                        onClick={() => tryMove(card.id, card.column + 1)}
                        aria-label={`Move ${card.company} to ${COLUMNS[card.column + 1].label}`}
                        className="flex size-5 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                      >
                        <ArrowRightIcon className="size-3" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )
        })}
      </div>
      <div className="mt-auto flex items-center gap-2 border-t pt-3 text-xs">
        <p
          role="status"
          aria-live="polite"
          className={cn('flex min-w-0 flex-1 items-center gap-1.5', feedback.tone === 'blocked' ? 'text-warning' : 'text-muted-foreground')}
        >
          {feedback.tone === 'blocked' ? (
            <OctagonAlertIcon aria-hidden className="size-3.5 shrink-0" />
          ) : (
            <CheckIcon aria-hidden className="size-3.5 shrink-0 text-success" />
          )}
          <span key={feedback.message} className="animate-quote line-clamp-2 text-left">
            {feedback.message}
          </span>
        </p>
        <button type="button" onClick={resetDemo} className="shrink-0 rounded-md px-2 py-1 font-medium text-primary hover:bg-accent">
          Reset
        </button>
      </div>
    </div>
  )
}
