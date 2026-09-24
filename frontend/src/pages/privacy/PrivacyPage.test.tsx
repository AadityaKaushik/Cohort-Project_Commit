import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PrivacyPage } from '@/pages/privacy/PrivacyPage'
import { getFlConsent, getFlSchema, optInFl, optOutFl, type FlConsent, type FlSchema } from '@/lib/fl-api'

vi.mock('@/lib/fl-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/fl-api')>('@/lib/fl-api')
  return {
    ...actual,
    getFlConsent: vi.fn(),
    getFlSchema: vi.fn(),
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
    vi.mocked(getFlSchema).mockResolvedValue(sampleSchema())
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
})
