import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApplicationsListPage } from '@/pages/applications/ApplicationsListPage'
import { listApplications, type ApplicationSummary, type PagedResponse } from '@/lib/applications-api'

vi.mock('@/lib/applications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-api')>('@/lib/applications-api')
  return { ...actual, listApplications: vi.fn() }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/applications']}>
        <ApplicationsListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleApplication(overrides: Partial<ApplicationSummary> = {}): ApplicationSummary {
  return {
    id: 'app-1',
    company: { id: 'co-1', name: 'Acme Corp', website: null, industry: null },
    job_title: 'Backend Engineer',
    status: 'APPLIED',
    current_stage: 'APPLIED',
    days_in_stage: 3,
    date_applied: '2026-01-10',
    priority: 'high',
    archived: false,
    allowed_next_statuses: [],
    correction_statuses: [],
    ...overrides,
  }
}

describe('ApplicationsListPage', () => {
  beforeEach(() => {
    vi.mocked(listApplications).mockReset()
  })

  it('shows a loading state before data arrives', () => {
    vi.mocked(listApplications).mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getAllByText('Applications')[0]).toBeInTheDocument()
  })

  it('shows an empty state with a create action when there are no applications', async () => {
    const empty: PagedResponse<ApplicationSummary> = { items: [], total: 0, page: 0, size: 25 }
    vi.mocked(listApplications).mockResolvedValue(empty)

    renderPage()

    expect(await screen.findByText('No applications yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /create your first application/i })).toBeInTheDocument()
  })

  it('shows an error state with a retry action when the request fails', async () => {
    vi.mocked(listApplications).mockRejectedValue(new Error('network down'))

    renderPage()

    expect(await screen.findByText(/couldn't load your applications/i)).toBeInTheDocument()
    const retryButton = screen.getByRole('button', { name: /try again/i })

    vi.mocked(listApplications).mockResolvedValue({ items: [], total: 0, page: 0, size: 25 })
    await userEvent.setup().click(retryButton)

    await waitFor(() => expect(listApplications).toHaveBeenCalledTimes(2))
  })

  it('renders a row per application once data loads', async () => {
    const page: PagedResponse<ApplicationSummary> = {
      items: [sampleApplication()],
      total: 1,
      page: 0,
      size: 25,
    }
    vi.mocked(listApplications).mockResolvedValue(page)

    renderPage()

    expect(await screen.findByText('Acme Corp')).toBeInTheDocument()
    expect(screen.getByText('Backend Engineer')).toBeInTheDocument()
    // Status badge and stage column both read "Applied" for this fixture.
    expect(within(screen.getByRole('table')).getAllByText('Applied')).toHaveLength(2)
  })
})
