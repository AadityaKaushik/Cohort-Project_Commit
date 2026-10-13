import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { OAuthCallbackPage } from '@/pages/auth/OAuthCallbackPage'

describe('OAuthCallbackPage', () => {
  it('reports a failure when the tokens are missing from the link', async () => {
    window.location.hash = ''
    render(
      <MemoryRouter>
        <OAuthCallbackPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Sign-in could not be completed.')
  })
})
