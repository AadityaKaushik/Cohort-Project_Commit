import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
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

// The first render pays a jsdom cold start that can pass 5s in a full parallel run.
describe('RegisterPage', { timeout: 20000 }, () => {
  beforeEach(() => {
    register.mockReset()
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: null,
      login: vi.fn(),
      register,
      confirmEmail: vi.fn(),
      logout: vi.fn(),
    })
  })

  it('submits the typed fields to register', async () => {
    register.mockResolvedValueOnce({ verificationRequired: false })
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

  it('sends a new account to enter its emailed code, with the address in router state', async () => {
    register.mockResolvedValueOnce({ verificationRequired: true })
    function CodePage() {
      const location = useLocation()
      return <p>code page for {(location.state as { email: string }).email}{location.search}</p>
    }
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<CodePage />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'correcthorsebattery')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('code page for jane@example.com')).toBeInTheDocument()
  })

  it('maps server field errors onto the offending inputs', async () => {
    register.mockRejectedValueOnce(
      new ApiError(400, 'One or more fields are invalid.', 'VALIDATION_ERROR', {
        password: 'is too common to be used as a password',
      }),
    )
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'passwordpassword')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('is too common to be used as a password')).toBeInTheDocument()
  })

  it('blocks a too-short password before calling the API', async () => {
    const user = userEvent.setup()
    renderRegisterPage()

    await user.type(screen.getByLabelText(/display name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'short')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/needs at least 10 characters/i)).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
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
