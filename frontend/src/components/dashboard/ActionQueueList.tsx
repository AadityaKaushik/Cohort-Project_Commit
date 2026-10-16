import { Link } from 'react-router-dom'
import { AlarmClockIcon, CalendarClockIcon, CheckCircle2Icon, ChevronRightIcon, MessageSquareWarningIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ActionItem, ActionItemType } from '@/lib/action-queue'
import { EmptyState } from '@/components/shared/EmptyState'

const ICONS: Record<ActionItemType, typeof AlarmClockIcon> = {
  stalled: AlarmClockIcon,
  upcoming_interview: CalendarClockIcon,
  missing_debrief: MessageSquareWarningIcon,
  due_task: CheckCircle2Icon,
}

const TONES: Record<ActionItemType, string> = {
  stalled: 'bg-warning-soft text-warning-soft-foreground',
  upcoming_interview: 'bg-primary/12 text-primary',
  missing_debrief: 'bg-info/12 text-info',
  due_task: 'bg-muted text-muted-foreground',
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
    <ul aria-label={label} className="divide-y overflow-hidden rounded-xl border bg-card">
      {items.map((item) => {
        const Icon = ICONS[item.type]
        return (
          <li key={item.id}>
            <Link to={item.href} className="group/row flex items-center gap-3 px-3 py-2.5 transition-colors hover:bg-accent">
              <span className={cn('flex size-8 shrink-0 items-center justify-center rounded-lg', TONES[item.type])}>
                <Icon aria-hidden className="size-4" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{item.title}</p>
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
