import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InterviewsListPage } from '@/pages/interviews/InterviewsListPage'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import type { PagedResponse } from '@/lib/applications-api'

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, listInterviews: vi.fn() }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/interviews']}>
        <InterviewsListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleInterview(overrides: Partial<InterviewSummary> = {}): InterviewSummary {
  return {
    id: 'interview-1',
    application_id: 'app-1',
    company_name: 'Acme Corp',
    job_title: 'Backend Engineer',
    round_number: 1,
    round_type: 'TECHNICAL',
    scheduled_at: '2026-02-01T10:00:00Z',
    completed_at: null,
    mode: 'video',
    outcome: 'pending',
    has_feedback: false,
    created_at: '2026-01-10T00:00:00Z',
    ...overrides,
  }
}

describe('InterviewsListPage', () => {
  beforeEach(() => {
    vi.mocked(listInterviews).mockReset()
  })

  it('shows empty states for both upcoming and past sections when there is no data', async () => {
    const empty: PagedResponse<InterviewSummary> = { items: [], total: 0, page: 0, size: 50 }
    vi.mocked(listInterviews).mockResolvedValue(empty)

    renderPage()

    expect(await screen.findByText('No upcoming interviews')).toBeInTheDocument()
    expect(await screen.findByText('No past interviews yet')).toBeInTheDocument()
  })

  it('splits interviews into upcoming and past sections via separate queries', async () => {
    vi.mocked(listInterviews).mockImplementation(({ upcoming } = {}) => {
      const item = sampleInterview({ id: upcoming ? 'upcoming-1' : 'past-1' })
      return Promise.resolve({ items: [item], total: 1, page: 0, size: 50 })
    })

    renderPage()

    await waitFor(() => expect(screen.getAllByText('Acme Corp')).toHaveLength(2))
    expect(listInterviews).toHaveBeenCalledWith(expect.objectContaining({ upcoming: true }))
    expect(listInterviews).toHaveBeenCalledWith(expect.objectContaining({ upcoming: false }))
  })

  it('offers a quick "Start debrief" action for a past interview with no feedback yet', async () => {
    vi.mocked(listInterviews).mockImplementation(({ upcoming } = {}) => {
      if (upcoming) {
        return Promise.resolve({ items: [], total: 0, page: 0, size: 50 })
      }
      return Promise.resolve({ items: [sampleInterview({ has_feedback: false })], total: 1, page: 0, size: 50 })
    })

    renderPage()

    expect(await screen.findByRole('button', { name: /start debrief/i })).toBeInTheDocument()
  })
})
