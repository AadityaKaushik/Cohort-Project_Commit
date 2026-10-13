import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import * as authApi from '@/lib/auth-api'

vi.mock('@/lib/auth-api')

describe('OAuthButtons', () => {
  beforeEach(() => {
    vi.mocked(authApi.listOAuthProviders).mockReset()
  })

  it('shows a button only for each configured provider', async () => {
    vi.mocked(authApi.listOAuthProviders).mockResolvedValue(['github'])
    render(<OAuthButtons />)

    const link = await screen.findByRole('link', { name: 'Continue with GitHub' })
    expect(link).toHaveAttribute('href', expect.stringContaining('/oauth2/authorization/github'))
    expect(screen.queryByRole('link', { name: 'Continue with Google' })).not.toBeInTheDocument()
  })

  it('renders nothing when no provider is configured', async () => {
    vi.mocked(authApi.listOAuthProviders).mockResolvedValue([])
    const { container } = render(<OAuthButtons />)

    await vi.waitFor(() => expect(authApi.listOAuthProviders).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })

  it('stays hidden when the provider list cannot be loaded', async () => {
    vi.mocked(authApi.listOAuthProviders).mockRejectedValue(new Error('offline'))
    const { container } = render(<OAuthButtons />)

    await vi.waitFor(() => expect(authApi.listOAuthProviders).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })
})
