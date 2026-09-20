import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage'
import {
  getConversionRates,
  getFunnel,
  getRejectionDistribution,
  getTimeline,
  type ConversionRates,
  type Funnel,
  type RejectionDistribution,
  type Timeline,
} from '@/lib/analytics-api'

vi.mock('@/lib/analytics-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/analytics-api')>('@/lib/analytics-api')
  return {
    ...actual,
    getFunnel: vi.fn(),
    getConversionRates: vi.fn(),
    getTimeline: vi.fn(),
    getRejectionDistribution: vi.fn(),
  }
})

function sampleFunnel(): Funnel {
  return { applied: 10, reached_oa: 6, reached_interview: 4, reached_final: 2, reached_offer: 1, sample_size: 10 }
}

function sampleRates(): ConversionRates {
  const rate = { value: 0.5, formatted: '50.0%', numerator: 5, denominator: 10, sufficient_data: true }
  return {
    application_to_oa: rate,
    oa_to_interview: rate,
    interview_to_final: rate,
    final_to_offer: rate,
    overall_success: rate,
  }
}

function sampleTimeline(): Timeline {
  return { buckets: [{ week_start: '2026-01-05', applications: 3, interviews: 1 }], sample_size: 4 }
}

function emptyTimeline(): Timeline {
  return { buckets: [], sample_size: 0 }
}

function sampleRejections(): RejectionDistribution {
  return { by_stage: { OA: 2, INTERVIEW: 1 }, no_response_count: 1, sample_size: 3, sufficient_data: false }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/analytics']}>
        <AnalyticsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AnalyticsPage', () => {
  beforeEach(() => {
    vi.mocked(getFunnel).mockReset()
    vi.mocked(getConversionRates).mockReset()
    vi.mocked(getTimeline).mockReset()
    vi.mocked(getRejectionDistribution).mockReset()
  })

  it('shows loading skeletons before any data arrives', () => {
    vi.mocked(getFunnel).mockReturnValue(new Promise(() => {}))
    vi.mocked(getConversionRates).mockReturnValue(new Promise(() => {}))
    vi.mocked(getTimeline).mockReturnValue(new Promise(() => {}))
    vi.mocked(getRejectionDistribution).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByTestId('funnel-loading')).toBeInTheDocument()
    expect(screen.getByTestId('conversions-loading')).toBeInTheDocument()
    expect(screen.getByTestId('timeline-loading')).toBeInTheDocument()
    expect(screen.getByTestId('rejections-loading')).toBeInTheDocument()
  })

  it('renders the funnel, conversion cards, timeline, and rejection distribution once loaded', async () => {
    vi.mocked(getFunnel).mockResolvedValue(sampleFunnel())
    vi.mocked(getConversionRates).mockResolvedValue(sampleRates())
    vi.mocked(getTimeline).mockResolvedValue(sampleTimeline())
    vi.mocked(getRejectionDistribution).mockResolvedValue(sampleRejections())

    renderPage()

    expect(await screen.findByTestId('funnel-chart')).toBeInTheDocument()
    expect(screen.getByText('Applied → OA')).toBeInTheDocument()
    expect(screen.getByTestId('activity-timeline-chart')).toBeInTheDocument()
    expect(screen.getByText('Rejected after OA')).toBeInTheDocument()
    expect(screen.getByText('No response (60+ days)')).toBeInTheDocument()
  })

  it('shows the empty state for the funnel and timeline when a user has no data yet', async () => {
    vi.mocked(getFunnel).mockResolvedValue({ applied: 0, reached_oa: 0, reached_interview: 0, reached_final: 0, reached_offer: 0, sample_size: 0 })
    vi.mocked(getConversionRates).mockResolvedValue(sampleRates())
    vi.mocked(getTimeline).mockResolvedValue(emptyTimeline())
    vi.mocked(getRejectionDistribution).mockResolvedValue({ by_stage: {}, no_response_count: 0, sample_size: 0, sufficient_data: false })

    renderPage()

    expect(await screen.findByText(/no applications yet/i)).toBeInTheDocument()
    expect(screen.getByText(/no activity in this range yet/i)).toBeInTheDocument()
    expect(screen.getByText(/no rejections recorded yet/i)).toBeInTheDocument()
  })
})
