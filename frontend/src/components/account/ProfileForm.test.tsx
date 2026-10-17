import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ProfileForm } from '@/components/account/ProfileForm'
import { useAuth } from '@/hooks/use-auth'
import * as usersApi from '@/lib/users-api'

const toast = vi.fn()
vi.mock('@/hooks/use-auth')
vi.mock('@/lib/users-api')
vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast }) }))

describe('ProfileForm', () => {
  beforeEach(() => {
    vi.mocked(usersApi.updateMyProfile).mockReset()
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: '1',
        email: 'nina@example.com',
        display_name: 'Nina',
        target_role: 'Backend engineer',
        target_date: null,
        graduation_year: 2027,
        timezone: null,
        onboarded: true,
      } as usersApi.UserProfile,
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      confirmEmail: vi.fn(),
      logout: vi.fn(),
    })
  })

  function renderForm() {
    return render(
      <QueryClientProvider client={new QueryClient()}>
        <ProfileForm />
      </QueryClientProvider>,
    )
  }

  it('shows the saved profile and saves edits', async () => {
    vi.mocked(usersApi.updateMyProfile).mockResolvedValue({} as usersApi.UserProfile)
    const user = userEvent.setup()
    renderForm()

    expect(screen.getByLabelText('Target role')).toHaveValue('Backend engineer')
    await user.clear(screen.getByLabelText('Display name'))
    await user.type(screen.getByLabelText('Display name'), 'Nina K')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    await waitFor(() =>
      expect(usersApi.updateMyProfile).toHaveBeenCalledWith(expect.objectContaining({ display_name: 'Nina K', graduation_year: 2027 })),
    )
  })

  it('blocks an invalid email before saving', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'nope')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByText(/valid email address/i)).toBeInTheDocument()
    expect(usersApi.updateMyProfile).not.toHaveBeenCalled()
  })

  it('says a code went to the new address after an email change', async () => {
    toast.mockReset()
    vi.mocked(usersApi.updateMyProfile).mockResolvedValue({} as usersApi.UserProfile)
    const user = userEvent.setup()
    renderForm()

    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'nina.new@example.com')
    await user.click(screen.getByRole('button', { name: 'Save profile' }))

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(expect.objectContaining({ description: expect.stringContaining('nina.new@example.com') })),
    )
  })
})
