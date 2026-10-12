import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PrivacyLedgerTable } from '@/components/privacy/PrivacyLedgerTable'
import type { PrivacyLedgerEntry } from '@/lib/fl-api'

vi.mock('@/hooks/use-mobile', () => ({ useIsMobile: () => true }))

const entry: PrivacyLedgerEntry = {
  id: 'e1',
  round_number: 7,
  created_at: '2026-01-01T00:00:00Z',
  cohort_size: 12,
  pre_clip_norm: 1.5,
  post_clip_norm: 1,
  noise_multiplier: 0.8,
  epsilon: 0.4,
  transmitted_vector: [0.1, 0.2],
} as PrivacyLedgerEntry

describe('PrivacyLedgerTable on a narrow screen', () => {
  it('renders each round as a card and still reveals the transmitted vector', async () => {
    const user = userEvent.setup()
    render(<PrivacyLedgerTable entries={[entry]} />)

    expect(screen.getByTestId('ledger-cards')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Expand vector for round 7' }))

    expect(screen.getByText('0.100000')).toBeInTheDocument()
  })
})
