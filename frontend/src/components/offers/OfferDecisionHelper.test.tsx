import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { OfferDecisionHelper } from '@/components/offers/OfferDecisionHelper'
import { getOfferDecision, saveOfferRatings, type OfferDecision } from '@/lib/offers-api'

vi.mock('@/lib/offers-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/offers-api')>('@/lib/offers-api')
  return { ...actual, getOfferDecision: vi.fn(), saveOfferRatings: vi.fn(), saveOfferWeights: vi.fn() }
})

const full = { pay: 4, growth: 3, learning: 3, work_life_balance: 3, location: 3, brand: 3 }

function decision(overrides: Partial<OfferDecision> = {}): OfferDecision {
  return {
    weights: { pay: 5 },
    offers: [
      { offer_id: 'o-1', company_name: 'Acme', job_title: 'Engineer', ratings: full, score: 68, rank: 1 },
      { offer_id: 'o-2', company_name: 'Globex', job_title: 'Engineer', ratings: {}, score: null, rank: null },
    ],
    leader_summary: null,
    ...overrides,
  }
}

function renderHelper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <OfferDecisionHelper offerIds={['o-1', 'o-2']} />
    </QueryClientProvider>,
  )
}

describe('OfferDecisionHelper', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows scores, asks for missing ratings and sends a rating on change', async () => {
    vi.mocked(getOfferDecision).mockResolvedValue(decision())
    vi.mocked(saveOfferRatings).mockResolvedValue(decision())
    renderHelper()

    expect(await screen.findByText('68 / 100')).toBeInTheDocument()
    expect(screen.getByText('Rate all six')).toBeInTheDocument()
    expect(screen.getByText(/Rate every factor on at least two offers/)).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Pay rating for Globex'), '4')
    await waitFor(() => expect(saveOfferRatings).toHaveBeenCalledWith('o-2', { pay: 4 }))
  })

  it('shows why the leading offer leads', async () => {
    vi.mocked(getOfferDecision).mockResolvedValue(
      decision({ leader_summary: 'Acme leads Globex by 9 points, mostly on pay (rated 5 against 2, weight 5).' }),
    )
    renderHelper()

    expect(await screen.findByTestId('decision-summary')).toHaveTextContent('Acme leads Globex by 9 points')
  })
})
