import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { CodeInput } from '@/components/auth/CodeInput'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import * as authApi from '@/lib/auth-api'

vi.mock('@/lib/auth-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/auth-api')>('@/lib/auth-api')
  return {
    ...actual,
    requestPasswordReset: vi.fn(),
    confirmPasswordReset: vi.fn(),
    requestEmailVerification: vi.fn(),
    confirmEmailVerification: vi.fn(),
  }
})
vi.mock('@/hooks/use-auth')

const confirmEmail = vi.fn()

function renderAt(path: string, state?: unknown) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: path, state }]}>
      <Routes>
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/" element={<p>signed in home</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

async function typeCode(user: ReturnType<typeof userEvent.setup>, code: string) {
  await user.click(screen.getByLabelText('Digit 1 of 6'))
  await user.keyboard(code)
}

describe('account recovery pages', { timeout: 20000 }, () => {
  beforeEach(() => {
    vi.mocked(authApi.requestPasswordReset).mockReset().mockResolvedValue(undefined)
    vi.mocked(authApi.confirmPasswordReset).mockReset().mockResolvedValue(undefined)
    vi.mocked(authApi.requestEmailVerification).mockReset().mockResolvedValue(undefined)
    confirmEmail.mockReset().mockResolvedValue(undefined)
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      confirmEmail,
      logout: vi.fn(),
    })
  })

  it('asks for a reset code and moves on to the code page whether or not the email exists', async () => {
    const user = userEvent.setup()
    renderAt('/forgot-password')

    await user.type(screen.getByLabelText(/email/i), 'nobody@example.com')
    await user.click(screen.getByRole('button', { name: /send code/i }))

    expect(authApi.requestPasswordReset).toHaveBeenCalledWith('nobody@example.com')
    expect(await screen.findByText(/if an account exists for nobody@example.com/i)).toBeInTheDocument()
  })

  it('submits the email, the code and the new password', async () => {
    const user = userEvent.setup()
    renderAt('/reset-password', { email: 'jane@example.com' })

    await typeCode(user, '482915')
    await user.type(screen.getByLabelText(/new password/i), 'a-long-enough-password')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    await waitFor(() =>
      expect(authApi.confirmPasswordReset).toHaveBeenCalledWith({
        email: 'jane@example.com',
        code: '482915',
        new_password: 'a-long-enough-password',
      }),
    )
    expect(await screen.findByRole('link', { name: /log in/i })).toBeInTheDocument()
  })

  it('blocks a short password or a partial code before calling the API', async () => {
    const user = userEvent.setup()
    renderAt('/reset-password', { email: 'jane@example.com' })

    await typeCode(user, '4829')
    await user.type(screen.getByLabelText(/new password/i), 'a-long-enough-password')
    await user.click(screen.getByRole('button', { name: /update password/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/all 6 digits/i)

    await typeCode(user, '482915')
    await user.clear(screen.getByLabelText(/new password/i))
    await user.type(screen.getByLabelText(/new password/i), 'short')
    await user.click(screen.getByRole('button', { name: /update password/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/at least 10/i)

    expect(authApi.confirmPasswordReset).not.toHaveBeenCalled()
  })

  it('shows the server message for a wrong reset code', async () => {
    vi.mocked(authApi.confirmPasswordReset).mockRejectedValue(new ApiError(400, 'That code is wrong or has expired.', 'INVALID_TOKEN'))
    const user = userEvent.setup()
    renderAt('/reset-password', { email: 'jane@example.com' })

    await typeCode(user, '000000')
    await user.type(screen.getByLabelText(/new password/i), 'a-long-enough-password')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('That code is wrong or has expired.')
  })

  it('confirms the email with the code and signs the user in', async () => {
    const user = userEvent.setup()
    renderAt('/verify-email', { email: 'jane@example.com' })

    expect(screen.getByText(/we sent a 6-digit code to jane@example.com/i)).toBeInTheDocument()
    await typeCode(user, '135790')
    await user.click(screen.getByRole('button', { name: /confirm email/i }))

    await waitFor(() => expect(confirmEmail).toHaveBeenCalledWith('jane@example.com', '135790'))
    expect(await screen.findByText('signed in home')).toBeInTheDocument()
  })

  it('asks for a fresh code when sent here from a blocked log-in', async () => {
    renderAt('/verify-email', { email: 'jane@example.com', sendCode: true })

    await waitFor(() => expect(authApi.requestEmailVerification).toHaveBeenCalledWith('jane@example.com'))
  })

  it('waits out the cooldown before offering a new code', () => {
    renderAt('/verify-email', { email: 'jane@example.com' })

    expect(screen.getByRole('button', { name: /send a new code in 60s/i })).toBeDisabled()
  })

  it('asks for the address when it was not carried over', () => {
    renderAt('/verify-email')

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
  })
})

describe('CodeInput', () => {
  it('fills every box from a pasted code and keeps digits only', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<CodeInput value="" onChange={onChange} />)

    await user.click(screen.getByLabelText('Digit 1 of 6'))
    await user.paste('12 34-56')

    expect(onChange).toHaveBeenLastCalledWith('123456')
  })

  it('ignores letters', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<CodeInput value="" onChange={onChange} />)

    await user.click(screen.getByLabelText('Digit 1 of 6'))
    await user.keyboard('a')

    expect(onChange).toHaveBeenLastCalledWith('')
  })
})
