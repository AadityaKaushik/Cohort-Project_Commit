import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AccountPage } from '@/pages/account/AccountPage'
import { changeMyPassword, deleteMyAccount, exportMyData } from '@/lib/users-api'
import { saveBlob } from '@/lib/download'
import { ApiError } from '@/lib/api-client'

const logout = vi.fn()

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({ logout, user: { email: 'me@example.com' } }),
}))
vi.mock('@/lib/users-api', () => ({
  changeMyPassword: vi.fn(),
  deleteMyAccount: vi.fn(),
  exportMyData: vi.fn(),
}))
vi.mock('@/lib/download', () => ({ saveBlob: vi.fn() }))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AccountPage />
    </QueryClientProvider>,
  )
}

describe('AccountPage', () => {
  beforeEach(() => logout.mockResolvedValue(undefined))
  afterEach(() => vi.clearAllMocks())

  it('changes the password and signs the user out', async () => {
    vi.mocked(changeMyPassword).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Current password'), 'old-password-1')
    await user.type(screen.getByLabelText('New password'), 'a-brand-new-passphrase')
    await user.click(screen.getByRole('button', { name: 'Change password' }))

    await waitFor(() =>
      expect(changeMyPassword).toHaveBeenCalledWith({ current_password: 'old-password-1', new_password: 'a-brand-new-passphrase' }),
    )
    await waitFor(() => expect(logout).toHaveBeenCalled())
  })

  it('shows the server message when the current password is wrong', async () => {
    vi.mocked(changeMyPassword).mockRejectedValue(new ApiError(400, 'The current password is incorrect.', 'VALIDATION_ERROR'))
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Current password'), 'wrong-password')
    await user.type(screen.getByLabelText('New password'), 'a-brand-new-passphrase')
    await user.click(screen.getByRole('button', { name: 'Change password' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The current password is incorrect.')
    expect(logout).not.toHaveBeenCalled()
  })

  it('downloads the data export', async () => {
    vi.mocked(exportMyData).mockResolvedValue({ blob: new Blob(['{}']), filename: 'cohort-data-export.json' })
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Download my data' }))

    await waitFor(() => expect(saveBlob).toHaveBeenCalledWith(expect.any(Blob), 'cohort-data-export.json'))
  })

  it('keeps deletion disabled until DELETE is typed', async () => {
    vi.mocked(deleteMyAccount).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPage()

    const button = screen.getByRole('button', { name: 'Delete my account' })
    expect(button).toBeDisabled()
    await user.type(screen.getByLabelText(/type delete to confirm/i), 'DELETE')
    expect(button).toBeEnabled()
    await user.click(button)

    await waitFor(() => expect(deleteMyAccount).toHaveBeenCalledWith('DELETE'))
    await waitFor(() => expect(logout).toHaveBeenCalled())
  })
})
