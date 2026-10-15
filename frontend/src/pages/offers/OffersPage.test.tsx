import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { OffersPage } from '@/pages/offers/OffersPage'
import { countdownLabel, listOffers, type Offer } from '@/lib/offers-api'

vi.mock('@/lib/offers-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/offers-api')>('@/lib/offers-api')
  return {
    ...actual,
    listOffers: vi.fn(),
    getOfferDecision: vi.fn().mockResolvedValue({ weights: {}, offers: [], leader_summary: null }),
  }
})

vi.mock('@/lib/applications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-api')>('@/lib/applications-api')
  return { ...actual, listApplications: vi.fn().mockResolvedValue({ items: [], total: 0, page: 0, size: 100 }) }
})

function offer(overrides: Partial<Offer> = {}): Offer {
  return {
    id: 'o-1',
    application_id: 'a-1',
    company_name: 'Acme',
    job_title: 'Engineer',
    base_salary: 100000,
    bonus: 10000,
    equity_value: null,
    signing_bonus: null,
    first_year_total: 110000,
    currency: 'USD',
    location: 'Remote',
    work_mode: 'remote',
    benefits: null,
    notes: null,
    start_date: null,
    response_deadline: '2026-11-01',
    days_remaining: 5,
    status: 'pending',
    created_at: '2026-10-01T00:00:00Z',
    updated_at: '2026-10-01T00:00:00Z',
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <OffersPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('OffersPage', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows an empty state when there are no offers', async () => {
    vi.mocked(listOffers).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('No offers yet')).toBeInTheDocument()
  })

  it('lists offers with the days left', async () => {
    vi.mocked(listOffers).mockResolvedValue([offer()])
    renderPage()

    expect(await screen.findByText('Acme')).toBeInTheDocument()
    expect(screen.getByText('5 days left')).toBeInTheDocument()
    expect(screen.getByText(/\$110,000 in the first year/)).toBeInTheDocument()
  })

  it('compares two ticked offers side by side', async () => {
    vi.mocked(listOffers).mockResolvedValue([
      offer(),
      offer({ id: 'o-2', company_name: 'Globex', base_salary: 120000, bonus: 5000, first_year_total: 125000, days_remaining: 1 }),
    ])
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('checkbox', { name: 'Compare Acme' }))
    expect(screen.getByText('Tick two or more offers to compare them.')).toBeInTheDocument()
    await user.click(screen.getByRole('checkbox', { name: 'Compare Globex' }))

    const table = screen.getByRole('table', { name: 'Offer comparison' })
    expect(within(table).getByText('$125,000')).toBeInTheDocument()
    expect(within(table).getByText('1 day left')).toBeInTheDocument()
  })
})

describe('countdownLabel', () => {
  it('describes overdue, today and decided offers', () => {
    expect(countdownLabel({ days_remaining: -2, status: 'pending' })).toBe('Deadline passed 2 days ago')
    expect(countdownLabel({ days_remaining: 0, status: 'pending' })).toBe('Reply today')
    expect(countdownLabel({ days_remaining: null, status: 'accepted' })).toBe('Accepted')
  })
})
