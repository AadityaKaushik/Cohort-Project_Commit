import { Link } from 'react-router-dom'
import { CheckCircle2Icon, ChevronRightIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ActionItem, ActionItemType } from '@/lib/action-queue'
import { EmptyState } from '@/components/shared/EmptyState'

// Board status codes, so each row says what kind of nudge it is before you read it.
const CODES: Record<ActionItemType, { label: string; className: string }> = {
  stalled: { label: 'Delayed', className: 'bg-destructive text-white dark:text-black' },
  upcoming_interview: { label: 'Boarding', className: 'bg-signal text-signal-foreground' },
  missing_debrief: { label: 'Debrief', className: 'bg-line-applied text-white dark:text-black' },
  due_task: { label: 'Prep due', className: 'bg-foreground text-background' },
}

export function ActionQueueList({ items, label = 'Next actions' }: { items: ActionItem[]; label?: string }) {
  if (items.length === 0) {
    return (
      <EmptyState
        compact
        icon={CheckCircle2Icon}
        title="Nothing needs attention right now"
        description="No stalled applications, missing debriefs or overdue tasks."
      />
    )
  }

  return (
    <ul aria-label={label} className="divide-y overflow-hidden rounded-md border bg-card">
      {items.map((item) => {
        const code = CODES[item.type]
        return (
          <li key={item.id}>
            <Link to={item.href} className="row-interactive group/row flex items-center gap-3 px-3 py-2.5">
              <span className={cn('board-label w-[5.5rem] shrink-0 rounded-[3px] px-1.5 py-1 text-center text-[10px] font-semibold', code.className)}>
                {code.label}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold">{item.title}</p>
                <p className="truncate text-xs text-muted-foreground">{item.subtitle}</p>
              </div>
              <ChevronRightIcon
                aria-hidden
                className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-hover/row:translate-x-0.5"
              />
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
