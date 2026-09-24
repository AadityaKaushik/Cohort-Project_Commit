import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RecommendationsPage } from '@/pages/recommendations/RecommendationsPage'
import {
  acceptRecommendation,
  dismissRecommendation,
  listRecommendations,
  snoozeRecommendation,
  type Recommendation,
} from '@/lib/recommendations-api'

vi.mock('@/lib/recommendations-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/recommendations-api')>('@/lib/recommendations-api')
  return {
    ...actual,
    listRecommendations: vi.fn(),
    acceptRecommendation: vi.fn(),
    snoozeRecommendation: vi.fn(),
    dismissRecommendation: vi.fn(),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/recommendations']}>
        <RecommendationsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleRecommendation(overrides: Partial<Recommendation> = {}): Recommendation {
  return {
    id: 'rec-1',
    type: 'weak_topic',
    tier: 'A',
    target_topic_id: 'topic-1',
    target_application_id: null,
    score: 0.7,
    headline: 'Practice Graphs',
    explanation: 'Mastery is at 30%, based on 6 observations, associated with weaker self-rated performance.',
    observation_count: 6,
    evidence: [{ label: 'Mastery score', detail: '30% mastery from 6 observations', occurred_at: '2026-01-01T00:00:00Z' }],
    status: 'active',
    snoozed_until: null,
    dismissed_reason: null,
    accepted_task_id: null,
    generated_at: '2026-01-10T00:00:00Z',
    federated_applied: false,
    rank_delta: 0,
    ...overrides,
  }
}

describe('RecommendationsPage', () => {
  beforeEach(() => {
    vi.mocked(listRecommendations).mockReset()
    vi.mocked(acceptRecommendation).mockReset()
    vi.mocked(snoozeRecommendation).mockReset()
    vi.mocked(dismissRecommendation).mockReset()
  })

  it('shows loading skeletons before data arrives', () => {
    vi.mocked(listRecommendations).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByText('Recommendations')).toBeInTheDocument()
  })

  it('shows an empty state when there are no recommendations', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([])

    renderPage()

    expect(await screen.findByText(/nothing to recommend right now/i)).toBeInTheDocument()
  })

  it('renders each recommendation card with its tier, headline, and evidence', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([sampleRecommendation()])

    renderPage()

    expect(await screen.findByText('Practice Graphs')).toBeInTheDocument()
    expect(screen.getByText(/mastery is at 30%/i)).toBeInTheDocument()
    expect(screen.getByText(/30% mastery from 6 observations/)).toBeInTheDocument()
    expect(screen.getByText(/Based on 6 observations/)).toBeInTheDocument()
  })

  it('accepts a recommendation', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([sampleRecommendation()])
    vi.mocked(acceptRecommendation).mockResolvedValue(sampleRecommendation({ status: 'accepted' }))

    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /accept/i }))

    await waitFor(() => expect(acceptRecommendation).toHaveBeenCalledWith('rec-1'))
  })

  it('snoozes a recommendation', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([sampleRecommendation()])
    vi.mocked(snoozeRecommendation).mockResolvedValue(sampleRecommendation({ status: 'snoozed' }))

    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /snooze 7 days/i }))

    await waitFor(() => expect(snoozeRecommendation).toHaveBeenCalledWith('rec-1'))
  })

  it('dismisses a recommendation with a reason', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([sampleRecommendation()])
    vi.mocked(dismissRecommendation).mockResolvedValue(sampleRecommendation({ status: 'dismissed' }))

    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^dismiss$/i }))
    await user.type(screen.getByLabelText(/why dismiss this/i), 'Not relevant right now')
    await user.click(screen.getByRole('button', { name: /confirm dismiss/i }))

    await waitFor(() =>
      expect(dismissRecommendation).toHaveBeenCalledWith('rec-1', 'Not relevant right now'),
    )
  })

  it('defaults to the local-only ranking', async () => {
    vi.mocked(listRecommendations).mockResolvedValue([sampleRecommendation()])

    renderPage()

    await screen.findByText('Practice Graphs')
    expect(listRecommendations).toHaveBeenCalledWith(false)
  })

  it('switching to Federated lift re-fetches with federated=true and shows the rank delta', async () => {
    vi.mocked(listRecommendations)
      .mockResolvedValueOnce([sampleRecommendation()])
      .mockResolvedValueOnce([sampleRecommendation({ federated_applied: true, rank_delta: 2 })])
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Practice Graphs')

    await user.click(screen.getByRole('button', { name: /federated lift/i }))

    await waitFor(() => expect(listRecommendations).toHaveBeenCalledWith(true))
    expect(await screen.findByText('+2')).toBeInTheDocument()
  })

  it('a zero rank delta shows no badge even in federated mode', async () => {
    vi.mocked(listRecommendations)
      .mockResolvedValueOnce([sampleRecommendation()])
      .mockResolvedValueOnce([sampleRecommendation({ federated_applied: true, rank_delta: 0 })])
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Practice Graphs')
    await user.click(screen.getByRole('button', { name: /federated lift/i }))

    await waitFor(() => expect(listRecommendations).toHaveBeenCalledWith(true))
    expect(screen.queryByTitle(/position change vs\. local-only ranking/i)).not.toBeInTheDocument()
  })

  it('says so when federated mode falls back to the local-only ranking', async () => {
    vi.mocked(listRecommendations)
      .mockResolvedValueOnce([sampleRecommendation()])
      .mockResolvedValueOnce([sampleRecommendation({ federated_applied: false, rank_delta: 0 })])
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Practice Graphs')
    expect(screen.queryByText('Shared model unavailable')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /federated lift/i }))

    expect(await screen.findByText('Shared model unavailable')).toBeInTheDocument()
  })
})
