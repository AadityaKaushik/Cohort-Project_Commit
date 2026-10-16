import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BellIcon, CheckCheckIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { EmptyState } from '@/components/shared/EmptyState'
import { Pagination } from '@/components/shared/DataTable'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingCards } from '@/components/shared/QueryStates'
import { Segmented } from '@/components/shared/Segmented'
import { NotificationItem } from '@/components/notifications/NotificationItem'
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
    <div className="mx-auto max-w-3xl space-y-5">
      <PageHeader
        title="Notifications"
        description={unreadCount > 0 ? `${unreadCount} unread` : 'Everything has been read'}
        actions={
          <Button variant="outline" onClick={() => markAllMutation.mutate()} disabled={unreadCount === 0 || markAllMutation.isPending}>
            <CheckCheckIcon aria-hidden />
            {markAllMutation.isPending ? 'Marking…' : 'Mark all as read'}
          </Button>
        }
      />

      <Segmented label="Filter notifications" value={filter} options={FILTERS} onChange={selectFilter} />

      {listQuery.isLoading ? (
        <LoadingCards count={4} label="Loading notifications" className="[&>*]:p-3" />
      ) : listQuery.isError ? (
        <ErrorState
          title="Couldn't load notifications"
          description="Something went wrong while fetching your notifications."
          onRetry={() => listQuery.refetch()}
        />
      ) : items.length === 0 ? (
        <EmptyState
          icon={BellIcon}
          title={EMPTY_COPY[filter].title}
          description={EMPTY_COPY[filter].description}
          quote={filter === 'unread' ? 5 : undefined}
          action={
            filter === 'unread' ? (
              <Button asChild>
                <Link to="/">Back to dashboard</Link>
              </Button>
            ) : (
              <Button asChild>
                <Link to="/applications">View applications</Link>
              </Button>
            )
          }
        />
      ) : (
        <div className="surface-raised overflow-hidden rounded-xl border p-1.5">
          <ul className="divide-y">
            {items.map((notification) => (
              <li key={notification.id} className="py-0.5">
                <NotificationItem notification={notification} onOpen={handleOpen} expanded />
              </li>
            ))}
          </ul>
        </div>
      )}

      {pageCount > 1 && <Pagination page={page} totalPages={pageCount} onPageChange={setPage} label="Notification pages" />}
    </div>
  )
}
