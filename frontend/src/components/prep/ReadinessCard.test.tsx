import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ReadinessCard } from '@/components/prep/ReadinessCard'
import * as prepApi from '@/lib/prep-api'

vi.mock('@/lib/prep-api')

function renderCard() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <ReadinessCard applicationId="a1" />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ReadinessCard', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows the score and the topics behind it', async () => {
    vi.mocked(prepApi.getReadiness).mockResolvedValue({
      score: 0.7,
      formatted: '70%',
      sample_size: 2,
      sufficient_data: true,
      topics: [
        { topic_id: 't1', topic_name: 'Arrays', weight: 3, mastery_score: 0.8, has_evidence: true },
        { topic_id: 't2', topic_name: 'Graphs', weight: 1, mastery_score: 0.4, has_evidence: true },
      ],
    })
    renderCard()
    expect(await screen.findByText('70%', { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByText('Arrays')).toBeInTheDocument()
  })

  it('suppresses the score when evidence is thin', async () => {
    vi.mocked(prepApi.getReadiness).mockResolvedValue({
      score: null,
      formatted: 'insufficient data',
      sample_size: 1,
      sufficient_data: false,
      topics: [{ topic_id: 't1', topic_name: 'Arrays', weight: 3, mastery_score: 0.8, has_evidence: true }],
    })
    renderCard()
    expect(await screen.findByText(/Not enough practice/)).toBeInTheDocument()
    expect(screen.queryByText('insufficient data')).not.toBeInTheDocument()
  })
})
