import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PrivacyLedgerTable } from '@/components/privacy/PrivacyLedgerTable'
import type { PrivacyLedgerEntry } from '@/lib/fl-api'

function entry(overrides: Partial<PrivacyLedgerEntry> = {}): PrivacyLedgerEntry {
  return {
    id: 'entry-1',
    round_number: 3,
    schema_version: 1,
    pre_clip_norm: 1.732,
    post_clip_norm: 1,
    noise_multiplier: 1,
    cohort_size: 5,
    epsilon: 1,
    transmitted_vector: [0.123456, -0.987654],
    created_at: '2026-01-10T00:00:00Z',
    ...overrides,
  }
}

describe('PrivacyLedgerTable', () => {
  it('shows one row per entry with its round metadata', () => {
    render(<PrivacyLedgerTable entries={[entry(), entry({ id: 'entry-2', round_number: 4, cohort_size: 7, pre_clip_norm: 0.5 })]} />)

    expect(screen.getByRole('button', { name: 'Expand vector for round 3' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Expand vector for round 4' })).toBeInTheDocument()
    expect(screen.getByText('1.7320')).toBeInTheDocument()
    expect(screen.getByText('0.5000')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
  })

  it('keeps the vector hidden until a row is expanded, then shows the real values', async () => {
    const user = userEvent.setup()
    render(<PrivacyLedgerTable entries={[entry()]} />)

    expect(screen.queryByText('0.123456')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Expand vector for round 3' }))

    expect(screen.getByText('0.123456')).toBeInTheDocument()
    expect(screen.getByText('-0.987654')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Collapse vector for round 3' })).toHaveAttribute('aria-expanded', 'true')
  })

  it('collapses an expanded row when it is clicked again', async () => {
    const user = userEvent.setup()
    render(<PrivacyLedgerTable entries={[entry()]} />)

    await user.click(screen.getByRole('button', { name: 'Expand vector for round 3' }))
    await user.click(screen.getByRole('button', { name: 'Collapse vector for round 3' }))

    expect(screen.queryByText('0.123456')).not.toBeInTheDocument()
  })
})
