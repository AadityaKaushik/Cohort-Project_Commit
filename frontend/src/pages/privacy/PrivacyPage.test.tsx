import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PrivacyPage } from '@/pages/privacy/PrivacyPage'
import {
  getFlConsent,
  getFlSchema,
  listPrivacyLedger,
  optInFl,
  optOutFl,
  type FlConsent,
  type FlSchema,
} from '@/lib/fl-api'

vi.mock('@/lib/fl-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/fl-api')>('@/lib/fl-api')
  return {
    ...actual,
    getFlConsent: vi.fn(),
    getFlSchema: vi.fn(),
    listPrivacyLedger: vi.fn(),
    optInFl: vi.fn(),
    optOutFl: vi.fn(),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/privacy']}>
        <PrivacyPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleSchema(): FlSchema {
  return {
    schema_version: 1,
    feature_count: 2,
    features: [
      { name: 'mastery_mean', lower_bound: 0, upper_bound: 1 },
      { name: 'confidence_trend', lower_bound: -1, upper_bound: 1 },
    ],
    not_collected: ['free text', 'resume content'],
  }
}

describe('PrivacyPage', () => {
  beforeEach(() => {
    vi.mocked(getFlConsent).mockReset()
    vi.mocked(getFlSchema).mockReset()
    vi.mocked(optInFl).mockReset()
    vi.mocked(optOutFl).mockReset()
    vi.mocked(listPrivacyLedger).mockReset()
    vi.mocked(getFlSchema).mockResolvedValue(sampleSchema())
    vi.mocked(listPrivacyLedger).mockResolvedValue([])
  })

  it('shows opted-out state and the opt-in action by default', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: false } satisfies FlConsent)

    renderPage()

    expect(await screen.findByText('Opted out')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /opt in to federated learning/i })).toBeInTheDocument()
  })

  it('shows opted-in state and the withdraw action when already opted in', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: true } satisfies FlConsent)

    renderPage()

    expect(await screen.findByText('Opted in')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /withdraw from federation/i })).toBeInTheDocument()
  })

  it('publishes the actual feature schema, not a paraphrase', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: false })

    renderPage()

    expect(await screen.findByText('mastery_mean')).toBeInTheDocument()
    expect(screen.getByText('confidence_trend')).toBeInTheDocument()
    expect(screen.getByText('free text')).toBeInTheDocument()
    expect(screen.getByText('resume content')).toBeInTheDocument()
  })

  it('opting in calls the API and flips the displayed state', async () => {
    vi.mocked(getFlConsent).mockResolvedValueOnce({ opted_in: false }).mockResolvedValueOnce({ opted_in: true })
    vi.mocked(optInFl).mockResolvedValue({ opted_in: true })
    const user = userEvent.setup()

    renderPage()
    await screen.findByText('Opted out')

    await user.click(screen.getByRole('button', { name: /opt in to federated learning/i }))

    await waitFor(() => expect(optInFl).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('Opted in')).toBeInTheDocument()
  })

  it('says nothing has been sent when the ledger is empty', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: false })

    renderPage()

    expect(await screen.findByText('Nothing has been sent yet.')).toBeInTheDocument()
  })

  it('shows ledger rows and inspects a transmitted vector with schema feature names', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: true })
    vi.mocked(listPrivacyLedger).mockResolvedValue([
      {
        id: 'entry-1',
        round_number: 2,
        schema_version: 1,
        pre_clip_norm: 1.5,
        post_clip_norm: 1,
        noise_multiplier: 1,
        cohort_size: 4,
        epsilon: 1,
        transmitted_vector: [0.25, -0.75],
        created_at: '2026-01-10T00:00:00Z',
      },
    ])
    const user = userEvent.setup()

    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Expand vector for round 2' }))

    expect(screen.getByText('0.250000')).toBeInTheDocument()
    expect(screen.getByText('-0.750000')).toBeInTheDocument()
    expect(screen.getAllByText('mastery_mean')).toHaveLength(2)
  })

  it('shows an error when the ledger cannot be loaded', async () => {
    vi.mocked(getFlConsent).mockResolvedValue({ opted_in: false })
    vi.mocked(listPrivacyLedger).mockRejectedValue(new Error('boom'))

    renderPage()

    expect(await screen.findByText(/couldn.t load the privacy ledger/i)).toBeInTheDocument()
  })
})
