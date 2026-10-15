import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ReviewTodayCard } from '@/components/prep/ReviewTodayCard'
import * as prepApi from '@/lib/prep-api'

vi.mock('@/lib/prep-api')

function renderCard(limit?: number) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <ReviewTodayCard limit={limit} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function item(id: string, name: string): prepApi.ReviewItem {
  return {
    topic_id: id,
    topic_name: name,
    question_id: `q-${id}`,
    question_text: 'Question',
    due_date: '2026-03-05',
    days_overdue: 5,
    mastery_score: 0.4,
    last_practised_at: '2026-03-04T10:00:00Z',
    reason: 'Last practised 6 days ago, mastery 40%',
  }
}

describe('ReviewTodayCard', () => {
  beforeEach(() => vi.resetAllMocks())

  it('lists due topics with the reason and a practise link to the question', async () => {
    vi.mocked(prepApi.getReviewQueue).mockResolvedValue({ as_of: '2026-03-10', items: [item('t1', 'Graphs')] })
    renderCard()

    expect(await screen.findByText('Graphs')).toBeInTheDocument()
    expect(screen.getByText('Last practised 6 days ago, mastery 40%')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Practise Graphs' })).toHaveAttribute(
      'href',
      '/prep/questions?topic_id=t1&practise=q-t1',
    )
  })

  it('shows an empty state when nothing is due', async () => {
    vi.mocked(prepApi.getReviewQueue).mockResolvedValue({ as_of: '2026-03-10', items: [] })
    renderCard()

    expect(await screen.findByText(/Nothing to review today/)).toBeInTheDocument()
  })

  it('caps the list and links to the full queue', async () => {
    vi.mocked(prepApi.getReviewQueue).mockResolvedValue({
      as_of: '2026-03-10',
      items: [item('t1', 'Graphs'), item('t2', 'Arrays'), item('t3', 'Heaps')],
    })
    renderCard(2)

    expect(await screen.findByText('Graphs')).toBeInTheDocument()
    expect(screen.queryByText('Heaps')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See all 3' })).toHaveAttribute('href', '/prep')
  })
})
