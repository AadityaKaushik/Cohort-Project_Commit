import { apiFetch } from '@/lib/api-client'
import type { PagedResponse } from '@/lib/applications-api'

export type NotificationType =
  | 'stalled_application'
  | 'upcoming_interview'
  | 'missing_debrief'
  | 'new_recommendation'
  | 'fl_round_result'

export interface AppNotification {
  id: string
  type: NotificationType
  title: string
  body: string
  related_entity_type: 'application' | 'interview' | null
  related_entity_id: string | null
  is_read: boolean
  read_at: string | null
  created_at: string
}

export type NotificationFilter = 'all' | 'unread' | 'read'

export function listNotifications(params: { status?: NotificationFilter; page?: number; size?: number } = {}) {
  const query = new URLSearchParams()
  if (params.status) query.set('status', params.status)
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  const queryString = query.toString()
  return apiFetch<PagedResponse<AppNotification>>(`/notifications${queryString ? `?${queryString}` : ''}`)
}

export function getUnreadNotificationCount() {
  return apiFetch<{ unread_count: number }>('/notifications/unread-count')
}

export function markNotificationRead(id: string) {
  return apiFetch<AppNotification>(`/notifications/${id}/read`, { method: 'POST' })
}

export function markAllNotificationsRead() {
  return apiFetch<{ marked_read: number }>('/notifications/read-all', { method: 'POST' })
}

export function notificationHref(notification: Pick<AppNotification, 'type' | 'related_entity_type' | 'related_entity_id'>): string | null {
  const { type, related_entity_type: entityType, related_entity_id: entityId } = notification
  if (!entityType || !entityId) return null
  if (entityType === 'application') return `/applications/${entityId}`
  if (type === 'missing_debrief') return `/interviews/${entityId}/debrief`
  return `/interviews/${entityId}`
}

export const notificationKeys = {
  all: ['notifications'] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
  list: (status: NotificationFilter, size: number) => ['notifications', 'list', status, size] as const,
}

// The bell badge's text: exact up to 99, then "99+" so the badge never grows unbounded.
export function formatUnreadBadge(count: number): string {
  return count > 99 ? '99+' : String(count)
}
