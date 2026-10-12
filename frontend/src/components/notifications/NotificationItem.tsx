import { Link } from 'react-router-dom'
import { formatDistanceToNow } from 'date-fns'
import {
  AlarmClockIcon,
  BellIcon,
  CalendarClockIcon,
  MessageSquareWarningIcon,
  ShieldCheckIcon,
  SparklesIcon,
  type LucideIcon,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { notificationHref, type AppNotification, type NotificationType } from '@/lib/notifications-api'

const ICONS: Record<NotificationType, LucideIcon> = {
  stalled_application: AlarmClockIcon,
  upcoming_interview: CalendarClockIcon,
  missing_debrief: MessageSquareWarningIcon,
  new_recommendation: SparklesIcon,
  fl_round_result: ShieldCheckIcon,
}

interface NotificationItemProps {
  notification: AppNotification
  /** Called when the user follows the notification's link (or clicks an unlinked one). */
  onOpen: (notification: AppNotification) => void
  /** Show the full body instead of clamping it to two lines (the full page does; the bell doesn't). */
  expanded?: boolean
}

/** One notification row, shared by the bell's dropdown and the full notifications page. */
export function NotificationItem({ notification, onOpen, expanded = false }: NotificationItemProps) {
  const Icon = ICONS[notification.type] ?? BellIcon
  const href = notificationHref(notification)
  const unread = !notification.is_read

  const content = (
    <>
      <span
        className={cn(
          'mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md',
          unread ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground',
        )}
        aria-hidden="true"
      >
        <Icon className="h-4 w-4" />
      </span>
      <span className="min-w-0 flex-1 space-y-0.5">
        <span className={cn('block text-sm', unread ? 'font-semibold' : 'font-medium text-muted-foreground')}>
          {notification.title}
        </span>
        <span className={cn('block text-xs text-muted-foreground', !expanded && 'line-clamp-2')}>{notification.body}</span>
        <span className="block text-xs text-muted-foreground">
          <time dateTime={notification.created_at}>
            {formatDistanceToNow(new Date(notification.created_at), { addSuffix: true })}
          </time>
        </span>
      </span>
      {unread && <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-primary" aria-label="Unread" />}
    </>
  )

  const className = 'flex w-full items-start gap-3 rounded-md p-2.5 text-left transition-colors hover:bg-accent focus-visible:bg-accent focus-visible:outline-none'

  if (href) {
    return (
      <Link to={href} className={className} onClick={() => onOpen(notification)}>
        {content}
      </Link>
    )
  }
  return (
    <button type="button" className={className} onClick={() => onOpen(notification)}>
      {content}
    </button>
  )
}
