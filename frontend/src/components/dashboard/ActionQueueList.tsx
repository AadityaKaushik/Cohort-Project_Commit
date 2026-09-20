import { Link } from 'react-router-dom'
import { AlarmClockIcon, CalendarClockIcon, CheckCircle2Icon, MessageSquareWarningIcon } from 'lucide-react'
import type { ActionItem, ActionItemType } from '@/lib/action-queue'
import { EmptyState } from '@/components/shared/EmptyState'

const ICONS: Record<ActionItemType, typeof AlarmClockIcon> = {
  stalled: AlarmClockIcon,
  upcoming_interview: CalendarClockIcon,
  missing_debrief: MessageSquareWarningIcon,
  due_task: CheckCircle2Icon,
}

/** US-49: "every item is one click from its action" -- each row is a `Link` straight to the record it concerns. */
export function ActionQueueList({ items }: { items: ActionItem[] }) {
  if (items.length === 0) {
    return (
      <EmptyState
        icon={CheckCircle2Icon}
        title="Nothing needs attention right now"
        description="Stalled applications, missing debriefs, upcoming interviews, and overdue prep tasks will show up here."
      />
    )
  }

  return (
    <ul className="divide-y rounded-md border">
      {items.map((item) => {
        const Icon = ICONS[item.type]
        return (
          <li key={item.id}>
            <Link to={item.href} className="flex items-start gap-3 p-3 transition-colors hover:bg-accent">
              <Icon className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{item.title}</p>
                <p className="truncate text-xs text-muted-foreground">{item.subtitle}</p>
              </div>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
