import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NotificationBell } from '@/components/notifications/NotificationBell'
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

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
}

function renderBell() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <NotificationBell />
        <Routes>
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function notification(overrides: Partial<AppNotification> = {}): AppNotification {
  return {
    id: 'n-1',
    type: 'stalled_application',
    title: 'Your Acme application has stalled',
    body: '"Backend Engineer" has been at the Applied stage for 20 days.',
    related_entity_type: 'application',
    related_entity_id: 'app-1',
    is_read: false,
    read_at: null,
    created_at: new Date().toISOString(),
    ...overrides,
  }
}

function page(items: AppNotification[]) {
  return { items, total: items.length, page: 0, size: 10 }
}

describe('NotificationBell', () => {
  beforeEach(() => {
    vi.mocked(getUnreadNotificationCount).mockResolvedValue({ unread_count: 2 })
    vi.mocked(listNotifications).mockResolvedValue(
      page([
        notification(),
        notification({
          id: 'n-2',
          type: 'missing_debrief',
          title: 'Debrief your Globex interview',
          related_entity_type: 'interview',
          related_entity_id: 'int-9',
        }),
      ]),
    )
    vi.mocked(markNotificationRead).mockResolvedValue(notification({ is_read: true }))
    vi.mocked(markAllNotificationsRead).mockResolvedValue({ marked_read: 2 })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows the unread count on the bell and in its accessible name', async () => {
    renderBell()

    expect(await screen.findByRole('button', { name: 'Notifications, 2 unread' })).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('shows no badge when everything has been read', async () => {
    vi.mocked(getUnreadNotificationCount).mockResolvedValue({ unread_count: 0 })
    renderBell()

    await waitFor(() => expect(getUnreadNotificationCount).toHaveBeenCalled())
    expect(await screen.findByRole('button', { name: 'Notifications' })).toBeInTheDocument()
  })

  it('opens a dropdown listing unread notifications first', async () => {
    const user = userEvent.setup()
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))

    expect(await screen.findByText('Your Acme application has stalled')).toBeInTheDocument()
    expect(screen.getByText('Debrief your Globex interview')).toBeInTheDocument()
    expect(listNotifications).toHaveBeenCalledWith({ status: 'unread', size: 10 })
  })

  it('opening a notification marks it read and navigates to what it is about', async () => {
    const user = userEvent.setup()
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))
    await user.click(await screen.findByText('Debrief your Globex interview'))

    await waitFor(() => expect(markNotificationRead).toHaveBeenCalledWith('n-2', expect.anything()))
    expect(screen.getByTestId('location')).toHaveTextContent('/interviews/int-9/debrief')
  })

  it('marks everything read in one click', async () => {
    const user = userEvent.setup()
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))
    await user.click(await screen.findByRole('button', { name: /mark all as read/i }))

    await waitFor(() => expect(markAllNotificationsRead).toHaveBeenCalledTimes(1))
  })

  it('switches to the "All" view and shows a friendly empty state', async () => {
    vi.mocked(listNotifications).mockImplementation(async (params) =>
      params?.status === 'all' ? page([]) : page([notification()]),
    )
    const user = userEvent.setup()
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))
    await user.click(await screen.findByRole('button', { name: 'All' }))

    expect(await screen.findByText('No notifications yet')).toBeInTheDocument()
    expect(listNotifications).toHaveBeenCalledWith({ status: 'all', size: 10 })
  })

  it('offers a retry when the list fails to load', async () => {
    vi.mocked(listNotifications).mockRejectedValue(new Error('boom'))
    const user = userEvent.setup()
    renderBell()

    await user.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))

    expect(await screen.findByText("We couldn't load your notifications.")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
