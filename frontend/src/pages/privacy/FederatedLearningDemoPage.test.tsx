import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { FederatedLearningDemoPage } from '@/pages/privacy/FederatedLearningDemoPage'
import { ApiError } from '@/lib/api-client'
import * as demoApi from '@/lib/fl-demo-api'
import type { FlDemoStatus } from '@/lib/fl-demo-api'

vi.mock('@/lib/fl-demo-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/fl-demo-api')>('@/lib/fl-demo-api')
  return { ...actual, getFlDemoStatus: vi.fn(), setSyntheticClients: vi.fn(), runFlDemoRound: vi.fn(), resetFlDemo: vi.fn() }
})

const NAMES = Array.from({ length: 24 }, (_, i) => (i === 0 ? 'coverage_ratio' : i === 1 ? 'hard_ratio' : `feature_${i}`))

function status(overrides: Partial<FlDemoStatus> = {}): FlDemoStatus {
  return {
    available: true,
    unavailable_reason: null,
    opted_in: true,
    your_rated_rounds: 9,
    minimum_rated_rounds: 5,
    your_predicted_confidence: 0.42,
    synthetic_clients: 20,
    noise_levels: [1, 0.5, 0.25],
    central_ceiling: 0.74,
    model: {
      version: 3,
      round_number: 3,
      participant_count: 21,
      check_accuracy: 0.58,
      weights: NAMES.map((feature, i) => ({ feature, value: i === 0 ? 0.8 : i === 1 ? -0.5 : 0 })),
      intercept: -0.2,
    },
    versions: [0, 1, 2, 3].map((version) => ({ version, round_number: version, participant_count: 21, check_accuracy: 0.52 + version * 0.02 })),
    rounds: [
      { round_number: 4, created_at: '2026-10-18T10:00:00Z', accepted: false, reason: 'round 4 refused: 2 participant(s) is below the minimum cohort size of 3', cohort_size: 2, sat_out_count: 0, model_version: 3, check_accuracy: 0.58, noise_multiplier: 1 },
    ],
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <FederatedLearningDemoPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('FederatedLearningDemoPage', { timeout: 20000 }, () => {
  beforeEach(() => {
    vi.mocked(demoApi.getFlDemoStatus).mockReset().mockResolvedValue(status())
    vi.mocked(demoApi.runFlDemoRound).mockReset()
    vi.mocked(demoApi.setSyntheticClients).mockReset().mockResolvedValue(undefined)
    vi.mocked(demoApi.resetFlDemo).mockReset().mockResolvedValue(undefined)
  })

  it('shows the current model, its 24 weights with the strongest first, and your prediction', async () => {
    renderPage()

    const model = await screen.findByRole('region', { name: 'Current model' })
    expect(within(model).getByText('v3')).toBeInTheDocument()
    expect(within(model).getByText('58%')).toBeInTheDocument()
    expect(within(model).getByText('42%')).toBeInTheDocument()
    const bars = within(screen.getByRole('list', { name: 'Model weights' })).getAllByRole('listitem')
    expect(bars).toHaveLength(24)
    expect(bars[0]).toHaveAccessibleName('coverage ratio: 0.800')
    expect(bars[1]).toHaveAccessibleName('hard ratio: -0.500')
    expect(screen.getByText(/below the minimum cohort size of 3/)).toBeInTheDocument()
  })

  it('runs a round with the chosen noise and reports the new version', async () => {
    vi.mocked(demoApi.runFlDemoRound).mockResolvedValue({
      exported_users: 1, export_failures: 0, round_ran: true, round_number: 5, accepted: true, reason: null, cohort_size: 21, sat_out_count: 0, model_version: 4, check_accuracy: 0.6,
    })
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(await screen.findByLabelText('Noise'), '0.5')
    await user.click(screen.getByRole('button', { name: /run a round/i }))

    await waitFor(() => expect(demoApi.runFlDemoRound).toHaveBeenCalledWith(0.5))
    expect(await screen.findByText(/round 5 accepted: 21 people trained\. The model is now version 4, scoring 60%/i)).toBeInTheDocument()
  })

  it('runs five rounds in a row', async () => {
    vi.mocked(demoApi.runFlDemoRound).mockResolvedValue({
      exported_users: 1, export_failures: 0, round_ran: true, round_number: 9, accepted: true, reason: null, cohort_size: 21, sat_out_count: 0, model_version: 8, check_accuracy: 0.62,
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /run 5 rounds/i }))

    await waitFor(() => expect(demoApi.runFlDemoRound).toHaveBeenCalledTimes(5))
  })

  it('changes the number of synthetic people and resets to version 0', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(await screen.findByLabelText('Synthetic people'), '1')
    await waitFor(() => expect(demoApi.setSyntheticClients).toHaveBeenCalledWith(1))
    await user.click(screen.getByRole('button', { name: /reset to version 0/i }))
    await waitFor(() => expect(demoApi.resetFlDemo).toHaveBeenCalled())
  })

  it('explains a refused round', async () => {
    vi.mocked(demoApi.runFlDemoRound).mockResolvedValue({
      exported_users: 1, export_failures: 0, round_ran: true, round_number: 6, accepted: false, reason: 'round 6 refused: 2 participant(s) is below the minimum cohort size of 3. A cohort this small would not keep anyone anonymous', cohort_size: 2, sat_out_count: 0, model_version: 3, check_accuracy: 0.58,
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /run a round/i }))

    expect(await screen.findByText(/round 6 refused: 2 participant\(s\) is below the minimum cohort size of 3/i)).toBeInTheDocument()
  })

  it('says when the federated learning service is not running', async () => {
    vi.mocked(demoApi.getFlDemoStatus).mockResolvedValue(status({ available: false, model: null, versions: [] }))
    renderPage()

    expect(await screen.findByText('The federated learning service is not running')).toBeInTheDocument()
  })

  it('says the page needs the dev profile when the endpoint does not exist', async () => {
    vi.mocked(demoApi.getFlDemoStatus).mockRejectedValue(new ApiError(404, 'Not found'))
    renderPage()

    expect(await screen.findByText('This page only exists in the dev profile')).toBeInTheDocument()
  })

  it('says when a round could not reach the federated learning service', async () => {
    vi.mocked(demoApi.runFlDemoRound).mockResolvedValue({
      exported_users: 0, export_failures: 3, round_ran: false, round_number: null, accepted: null, reason: null, cohort_size: null, sat_out_count: null, model_version: null, check_accuracy: null,
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /run a round/i }))

    expect(await screen.findByText('No round ran: the federated learning service could not be reached.')).toBeInTheDocument()
  })
})
