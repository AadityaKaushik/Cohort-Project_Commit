import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { PrivacyPolicyPage } from '@/pages/privacy/PrivacyPolicyPage'

vi.mock('@/hooks/use-auth', () => ({ useAuth: () => ({ isAuthenticated: false }) }))
vi.mock('@/components/layout/ThemeToggle', () => ({ ThemeToggle: () => null }))

describe('PrivacyPolicyPage', () => {
  it('covers data, federated learning, the ledger, export and AI', () => {
    render(
      <MemoryRouter>
        <PrivacyPolicyPage />
      </MemoryRouter>,
    )

    for (const heading of [
      'What is stored',
      'Who can see it',
      'How federated learning works',
      'The Privacy Ledger',
      'Export and delete',
      'AI features',
    ]) {
      expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument()
    }
    expect(screen.getByText(/Nothing is sent automatically/)).toBeInTheDocument()
  })
})
