import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type AppNotification,
} from '@/lib/notifications-api'

vi.mock('@/lib/notifications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/notifications-api')>('@/lib/notifications-api')
  return {
    ...actual,
    getUnreadNotificationCount: vi.fn(),
    listNotifications: vi.fn(),
    markNotificationRead: vi.fn(),
    markAllNotificationsRead: vi.fn(),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function notification(overrides: Partial<AppNotification> = {}): AppNotification {
  return {
    id: 'n-1',
    type: 'upcoming_interview',
    title: 'Interview coming up with Globex',
    body: 'Round 2 (Technical) is scheduled for Sat 26 Sep, 14:00 (UTC).',
    related_entity_type: 'interview',
    related_entity_id: 'int-1',
    is_read: false,
    read_at: null,
    created_at: new Date().toISOString(),
    ...overrides,
  }
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.mocked(getUnreadNotificationCount).mockResolvedValue({ unread_count: 1 })
    vi.mocked(markNotificationRead).mockResolvedValue(notification({ is_read: true }))
    vi.mocked(markAllNotificationsRead).mockResolvedValue({ marked_read: 1 })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('starts on the unread view and shows the full notification text', async () => {
    vi.mocked(listNotifications).mockResolvedValue({ items: [notification()], total: 1, page: 0, size: 20 })
    renderPage()

    expect(await screen.findByText('Interview coming up with Globex')).toBeInTheDocument()
    expect(screen.getByText('1 unread')).toBeInTheDocument()
    expect(listNotifications).toHaveBeenCalledWith({ status: 'unread', page: 0, size: 20 })
    expect(screen.getByRole('link', { name: /interview coming up with globex/i })).toHaveAttribute('href', '/interviews/int-1')
  })

  it('shows read notifications separately (FR-101)', async () => {
    vi.mocked(listNotifications).mockImplementation(async (params) =>
      params?.status === 'read'
        ? { items: [notification({ id: 'n-2', title: 'An older one', is_read: true })], total: 1, page: 0, size: 20 }
        : { items: [notification()], total: 1, page: 0, size: 20 },
    )
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Interview coming up with Globex')
    await user.click(screen.getByRole('button', { name: 'Read' }))

    expect(await screen.findByText('An older one')).toBeInTheDocument()
    expect(screen.queryByText('Interview coming up with Globex')).not.toBeInTheDocument()
  })

  it('shows an all-caught-up empty state when nothing is unread', async () => {
    vi.mocked(getUnreadNotificationCount).mockResolvedValue({ unread_count: 0 })
    vi.mocked(listNotifications).mockResolvedValue({ items: [], total: 0, page: 0, size: 20 })
    renderPage()

    expect(await screen.findByText("You're all caught up")).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to dashboard' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('button', { name: /mark all as read/i })).toBeDisabled()
  })

  it('marks all as read', async () => {
    vi.mocked(listNotifications).mockResolvedValue({ items: [notification()], total: 1, page: 0, size: 20 })
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Interview coming up with Globex')
    await waitFor(() => expect(screen.getByRole('button', { name: /mark all as read/i })).toBeEnabled())
    await user.click(screen.getByRole('button', { name: /mark all as read/i }))

    await waitFor(() => expect(markAllNotificationsRead).toHaveBeenCalledTimes(1))
  })

  it('paginates when there is more than one page', async () => {
    vi.mocked(listNotifications).mockResolvedValue({ items: [notification()], total: 45, page: 0, size: 20 })
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText('Page 1 of 3')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Next' }))

    await waitFor(() => expect(listNotifications).toHaveBeenCalledWith({ status: 'unread', page: 1, size: 20 }))
  })

  it('shows an error state with a retry', async () => {
    vi.mocked(listNotifications).mockRejectedValue(new Error('boom'))
    renderPage()

    expect(await screen.findByText("Couldn't load notifications")).toBeInTheDocument()
  })
})
