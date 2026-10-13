import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { OnboardingPage } from '@/pages/onboarding/OnboardingPage'
import { useAuth } from '@/hooks/use-auth'
import * as usersApi from '@/lib/users-api'

vi.mock('@/hooks/use-auth')
vi.mock('@/lib/users-api')

function renderPage() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/onboarding']}>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/" element={<p>Dashboard</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.mocked(usersApi.completeOnboarding).mockReset()
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'n@example.com', display_name: 'Nina', onboarded: false } as usersApi.UserProfile,
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    })
  })

  it('blocks submission without a role and a future date', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Create my plan' }))

    expect(await screen.findByText('Target role is required.')).toBeInTheDocument()
    expect(usersApi.completeOnboarding).not.toHaveBeenCalled()
  })

  it('creates the plan and lands on the dashboard rather than an empty page', async () => {
    vi.mocked(usersApi.completeOnboarding).mockResolvedValue({ profile: {} as usersApi.UserProfile, tasks_created: 6 })
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Target role'), 'Backend engineer')
    await user.type(screen.getByLabelText('Be interview-ready by'), '2999-01-01')
    await user.click(screen.getByRole('button', { name: 'Create my plan' }))

    await waitFor(() =>
      expect(usersApi.completeOnboarding).toHaveBeenCalledWith({ target_role: 'Backend engineer', target_date: '2999-01-01' }),
    )
    expect(await screen.findByText('Dashboard')).toBeInTheDocument()
  })

  it('lets the user skip', async () => {
    vi.mocked(usersApi.completeOnboarding).mockResolvedValue({ profile: {} as usersApi.UserProfile, tasks_created: 0 })
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Skip for now' }))

    await waitFor(() => expect(usersApi.completeOnboarding).toHaveBeenCalledWith({ skip: true }))
  })
})
