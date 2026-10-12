import { describe, expect, it } from 'vitest'
import { formatUnreadBadge, notificationHref } from '@/lib/notifications-api'

describe('notificationHref', () => {
  it('links a stalled application to the application itself', () => {
    expect(
      notificationHref({ type: 'stalled_application', related_entity_type: 'application', related_entity_id: 'app-1' }),
    ).toBe('/applications/app-1')
  })

  it('links an upcoming interview to the interview', () => {
    expect(
      notificationHref({ type: 'upcoming_interview', related_entity_type: 'interview', related_entity_id: 'int-1' }),
    ).toBe('/interviews/int-1')
  })

  it('links a missing debrief straight to the debrief form', () => {
    expect(
      notificationHref({ type: 'missing_debrief', related_entity_type: 'interview', related_entity_id: 'int-2' }),
    ).toBe('/interviews/int-2/debrief')
  })

  it('returns null when the notification points at nothing', () => {
    expect(notificationHref({ type: 'fl_round_result', related_entity_type: null, related_entity_id: null })).toBeNull()
  })
})

describe('formatUnreadBadge', () => {
  it('shows exact counts up to 99 and caps above that', () => {
    expect(formatUnreadBadge(1)).toBe('1')
    expect(formatUnreadBadge(99)).toBe('99')
    expect(formatUnreadBadge(100)).toBe('99+')
  })
})
