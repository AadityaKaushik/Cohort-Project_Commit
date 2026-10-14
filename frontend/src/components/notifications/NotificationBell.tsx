import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BellIcon, CheckCheckIcon, InboxIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Skeleton } from '@/components/ui/skeleton'
import { NotificationItem } from '@/components/notifications/NotificationItem'
import { cn } from '@/lib/utils'
import {
  formatUnreadBadge,
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  notificationKeys,
  type AppNotification,
} from '@/lib/notifications-api'

const BELL_PAGE_SIZE = 10
const UNREAD_POLL_MS = 60_000

type BellView = 'unread' | 'all'

export function NotificationBell() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [view, setView] = useState<BellView>('unread')

  const countQuery = useQuery({
    queryKey: notificationKeys.unreadCount,
    queryFn: getUnreadNotificationCount,
    refetchInterval: UNREAD_POLL_MS,
  })
  const unreadCount = countQuery.data?.unread_count ?? 0

  const listQuery = useQuery({
    queryKey: notificationKeys.list(view, BELL_PAGE_SIZE),
    queryFn: () => listNotifications({ status: view, size: BELL_PAGE_SIZE }),
    enabled: open,
  })

  const invalidateAll = () => queryClient.invalidateQueries({ queryKey: notificationKeys.all })

  const markReadMutation = useMutation({ mutationFn: markNotificationRead, onSettled: invalidateAll })
  const markAllMutation = useMutation({ mutationFn: markAllNotificationsRead, onSettled: invalidateAll })

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (next) {
      void countQuery.refetch()
    }
  }

  function handleOpenNotification(notification: AppNotification) {
    if (!notification.is_read) {
      markReadMutation.mutate(notification.id)
    }
    setOpen(false)
  }

  const label = unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'
  const items = listQuery.data?.items ?? []

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label={label}>
          <BellIcon className="h-5 w-5" />
          {unreadCount > 0 && (
            <span
              aria-hidden="true"
              className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] leading-none font-semibold text-white"
            >
              {formatUnreadBadge(unreadCount)}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(24rem,calc(100vw-2rem))] gap-0 p-0">
        <div className="flex items-center justify-between gap-2 border-b px-3 py-2">
          <h2 className="text-sm font-semibold">Notifications</h2>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            onClick={() => markAllMutation.mutate()}
            disabled={unreadCount === 0 || markAllMutation.isPending}
          >
            <CheckCheckIcon className="h-3.5 w-3.5" />
            Mark all as read
          </Button>
        </div>

        <div className="flex gap-1 border-b px-3 py-1.5" role="group" aria-label="Filter notifications">
          {(['unread', 'all'] as const).map((option) => (
            <Button
              key={option}
              variant="ghost"
              size="sm"
              aria-pressed={view === option}
              className={cn('h-7 px-2 text-xs', view === option && 'bg-accent text-accent-foreground')}
              onClick={() => setView(option)}
            >
              {option === 'unread' ? `Unread${unreadCount > 0 ? ` (${formatUnreadBadge(unreadCount)})` : ''}` : 'All'}
            </Button>
          ))}
        </div>

        <div className="max-h-[min(26rem,60vh)] overflow-y-auto p-1.5">
          {listQuery.isLoading ? (
            <div className="space-y-2 p-1.5" aria-label="Loading notifications">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : listQuery.isError ? (
            <div className="space-y-2 p-4 text-center" role="alert">
              <p className="text-sm text-muted-foreground">We couldn't load your notifications.</p>
              <Button variant="outline" size="sm" onClick={() => listQuery.refetch()}>
                Try again
              </Button>
            </div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
              <InboxIcon className="h-6 w-6 text-muted-foreground" aria-hidden="true" />
              <p className="text-sm font-medium">{view === 'unread' ? "You're all caught up" : 'No notifications yet'}</p>
              <p className="max-w-64 text-xs text-muted-foreground">
                {view === 'unread'
                  ? 'New alerts about stalled applications, upcoming interviews and missing debriefs will appear here.'
                  : "We'll let you know when an application stalls, an interview is coming up, or a debrief is due."}
              </p>
            </div>
          ) : (
            <ul className="space-y-0.5">
              {items.map((notification) => (
                <li key={notification.id}>
                  <NotificationItem notification={notification} onOpen={handleOpenNotification} />
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="border-t p-1.5">
          <Button variant="ghost" size="sm" className="w-full text-xs" asChild>
            <Link to="/notifications" onClick={() => setOpen(false)}>
              View all notifications
            </Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
