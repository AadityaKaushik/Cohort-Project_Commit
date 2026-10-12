import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage'
import * as authApi from '@/lib/auth-api'

vi.mock('@/lib/auth-api')

describe('account recovery pages', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows the same confirmation whether or not the email exists', async () => {
    vi.mocked(authApi.requestPasswordReset).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'nobody@example.com')
    await user.click(screen.getByRole('button', { name: /send reset link/i }))

    expect(await screen.findByRole('status')).toHaveTextContent(/if an account exists/i)
  })

  it('submits the token from the link with the new password', async () => {
    vi.mocked(authApi.confirmPasswordReset).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/reset-password?token=abc123']}>
        <ResetPasswordPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/new password/i), 'a-long-enough-password')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    await waitFor(() =>
      expect(authApi.confirmPasswordReset).toHaveBeenCalledWith({
        token: 'abc123',
        new_password: 'a-long-enough-password',
      }),
    )
  })

  it('blocks a too-short password before calling the API', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/reset-password?token=abc123']}>
        <ResetPasswordPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/new password/i), 'short')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/at least 10/i)
    expect(authApi.confirmPasswordReset).not.toHaveBeenCalled()
  })

  it('verifies the email from the link token', async () => {
    vi.mocked(authApi.confirmEmailVerification).mockResolvedValue(undefined)
    render(
      <MemoryRouter initialEntries={['/verify-email?token=tok']}>
        <VerifyEmailPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/is verified/i)).toBeInTheDocument()
    expect(authApi.confirmEmailVerification).toHaveBeenCalledWith('tok')
  })
})
