import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from '@/pages/LoginPage'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'

vi.mock('@/hooks/use-auth')

const login = vi.fn()

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset()
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: null,
      login,
      register: vi.fn(),
      logout: vi.fn(),
    })
  })

  it('submits the typed credentials to login', async () => {
    login.mockResolvedValueOnce(undefined)
    const user = userEvent.setup()
    renderLoginPage()

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'correcthorsebattery')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    await waitFor(() => expect(login).toHaveBeenCalledWith('jane@example.com', 'correcthorsebattery'))
  })

  it('shows a generic 401 error without confirming whether the account exists', async () => {
    login.mockRejectedValueOnce(new ApiError(401, 'Invalid email or password.'))
    const user = userEvent.setup()
    renderLoginPage()

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'wrongpassword')
    await user.click(screen.getByRole('button', { name: /log in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
  })

  it('shows the session-expired banner when the auth context reports one', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: 'Your session has expired. Please log in again.',
      login,
      register: vi.fn(),
      logout: vi.fn(),
    })

    renderLoginPage()

    expect(screen.getByText('Session ended')).toBeInTheDocument()
    expect(screen.getByText('Your session has expired. Please log in again.')).toBeInTheDocument()
  })

  it('redirects away from the login page once authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login,
      register: vi.fn(),
      logout: vi.fn(),
    })

    renderLoginPage()

    expect(screen.queryByRole('heading', { name: /log in to cohort/i })).not.toBeInTheDocument()
  })
})
