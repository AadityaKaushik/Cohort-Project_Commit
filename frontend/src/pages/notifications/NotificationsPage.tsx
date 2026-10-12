import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangleIcon, BellIcon, CheckCheckIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import { NotificationItem } from '@/components/notifications/NotificationItem'
import { cn } from '@/lib/utils'
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  notificationKeys,
  type AppNotification,
  type NotificationFilter,
} from '@/lib/notifications-api'

const PAGE_SIZE = 20

const FILTERS: { value: NotificationFilter; label: string }[] = [
  { value: 'unread', label: 'Unread' },
  { value: 'read', label: 'Read' },
  { value: 'all', label: 'All' },
]

const EMPTY_COPY: Record<NotificationFilter, { title: string; description: string }> = {
  unread: {
    title: "You're all caught up",
    description: 'Nothing new. Alerts about stalled applications, upcoming interviews and missing debriefs appear here.',
  },
  read: {
    title: 'No read notifications',
    description: 'Notifications you open or mark as read will be kept here for reference.',
  },
  all: {
    title: 'No notifications yet',
    description: "We'll let you know when an application stalls, an interview is coming up, or a debrief is due.",
  },
}

/** US-79 / FR-101: the full notification centre, with unread and read shown as separate views. */
export function NotificationsPage() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<NotificationFilter>('unread')
  const [page, setPage] = useState(0)

  const listQuery = useQuery({
    queryKey: [...notificationKeys.list(filter, PAGE_SIZE), page],
    queryFn: () => listNotifications({ status: filter, page, size: PAGE_SIZE }),
  })
  const countQuery = useQuery({ queryKey: notificationKeys.unreadCount, queryFn: getUnreadNotificationCount })
  const unreadCount = countQuery.data?.unread_count ?? 0

  const invalidateAll = () => queryClient.invalidateQueries({ queryKey: notificationKeys.all })
  const markReadMutation = useMutation({ mutationFn: markNotificationRead, onSettled: invalidateAll })
  const markAllMutation = useMutation({ mutationFn: markAllNotificationsRead, onSettled: invalidateAll })

  function selectFilter(next: NotificationFilter) {
    setFilter(next)
    setPage(0)
  }

  function handleOpen(notification: AppNotification) {
    if (!notification.is_read) {
      markReadMutation.mutate(notification.id)
    }
  }

  const total = listQuery.data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))
  const items = listQuery.data?.items ?? []

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Notifications</h2>
          <p className="text-sm text-muted-foreground">
            {unreadCount > 0 ? `${unreadCount} unread` : 'Everything has been read'}
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => markAllMutation.mutate()}
          disabled={unreadCount === 0 || markAllMutation.isPending}
        >
          <CheckCheckIcon className="h-4 w-4" />
          {markAllMutation.isPending ? 'Marking…' : 'Mark all as read'}
        </Button>
      </div>

      <div className="flex gap-1" role="group" aria-label="Filter notifications">
        {FILTERS.map((option) => (
          <Button
            key={option.value}
            variant="ghost"
            size="sm"
            aria-pressed={filter === option.value}
            className={cn(filter === option.value && 'bg-accent text-accent-foreground')}
            onClick={() => selectFilter(option.value)}
          >
            {option.label}
          </Button>
        ))}
      </div>

      {listQuery.isLoading ? (
        <div className="space-y-2" aria-label="Loading notifications">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-16 w-full" />
          ))}
        </div>
      ) : listQuery.isError ? (
        <EmptyState
          icon={AlertTriangleIcon}
          title="Couldn't load notifications"
          description="Something went wrong while fetching your notifications."
          action={
            <Button variant="outline" onClick={() => listQuery.refetch()}>
              Try again
            </Button>
          }
        />
      ) : items.length === 0 ? (
        <EmptyState icon={BellIcon} title={EMPTY_COPY[filter].title} description={EMPTY_COPY[filter].description} />
      ) : (
        <Card className="py-0">
          <CardContent className="p-1.5">
            <ul className="divide-y">
              {items.map((notification) => (
                <li key={notification.id} className="py-0.5">
                  <NotificationItem notification={notification} onOpen={handleOpen} expanded />
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}

      {pageCount > 1 && (
        <nav className="flex items-center justify-between" aria-label="Notification pages">
          <Button variant="outline" size="sm" onClick={() => setPage((p) => p - 1)} disabled={page === 0}>
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {pageCount}
          </span>
          <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= pageCount}>
            Next
          </Button>
        </nav>
      )}
    </div>
  )
}
