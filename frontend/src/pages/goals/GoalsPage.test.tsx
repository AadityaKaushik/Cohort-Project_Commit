import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { GoalsPage } from '@/pages/goals/GoalsPage'
import { getActivity, listGoals, setGoal } from '@/lib/goals-api'

vi.mock('@/lib/goals-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/goals-api')>('@/lib/goals-api')
  return { ...actual, listGoals: vi.fn(), setGoal: vi.fn(), getActivity: vi.fn() }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <GoalsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('GoalsPage', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows progress for each goal and the streak', async () => {
    vi.mocked(listGoals).mockResolvedValue([
      { id: 'g-1', metric: 'applications', weekly_target: 5, progress: 3, achieved: false },
    ])
    vi.mocked(getActivity).mockResolvedValue({
      days: [
        { date: '2026-10-12', count: 2 },
        { date: '2026-10-13', count: 0 },
      ],
      current_streak: 4,
      longest_streak: 9,
    })
    renderPage()

    expect(await screen.findByText('3 of 5 this week')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Applications progress' })).toHaveAttribute('aria-valuenow', '3')
    expect(await screen.findByText(/Current streak 4 days/)).toBeInTheDocument()
    expect(screen.getByTitle('2026-10-12: 2 actions')).toBeInTheDocument()
  })

  it('sets a weekly goal', async () => {
    vi.mocked(listGoals).mockResolvedValue([])
    vi.mocked(getActivity).mockResolvedValue({ days: [], current_streak: 0, longest_streak: 0 })
    vi.mocked(setGoal).mockResolvedValue({ id: 'g-1', metric: 'interviews', weekly_target: 2, progress: 0, achieved: false })
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(await screen.findByLabelText('Track'), 'interviews')
    await user.clear(screen.getByLabelText('Per week'))
    await user.type(screen.getByLabelText('Per week'), '2')
    await user.click(screen.getByRole('button', { name: 'Set goal' }))

    expect(setGoal).toHaveBeenCalledWith('interviews', 2)
  })
})
