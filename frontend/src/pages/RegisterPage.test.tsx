import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { RegisterPage } from '@/pages/RegisterPage'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'

vi.mock('@/hooks/use-auth')

const register = vi.fn()

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>,
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    register.mockReset()
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: null,
      login: vi.fn(),
      register,
      logout: vi.fn(),
    })
  })

  it('submits the typed fields to register', async () => {
    register.mockResolvedValueOnce(undefined)
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'correcthorsebattery')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() =>
      expect(register).toHaveBeenCalledWith('jane@example.com', 'correcthorsebattery', 'Jane Doe'),
    )
  })

  it('maps server field errors onto the offending inputs', async () => {
    register.mockRejectedValueOnce(
      new ApiError(400, 'One or more fields are invalid.', 'VALIDATION_ERROR', {
        password: 'must be at least 10 characters',
      }),
    )
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'short')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('must be at least 10 characters')).toBeInTheDocument()
  })

  it('shows a generic conflict message without confirming the email is registered', async () => {
    register.mockRejectedValueOnce(new ApiError(409, 'Unable to complete registration.'))
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'correcthorsebattery')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to complete registration.')
  })
})
