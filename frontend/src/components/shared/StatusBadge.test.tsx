import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from '@/components/shared/StatusBadge'

describe('StatusBadge', () => {
  it('always renders a text label, never colour alone (NFR-47)', () => {
    render(<StatusBadge status="INTERVIEW" />)
    expect(screen.getByText('Interview')).toBeInTheDocument()
  })

  it('renders a distinct label for each status', () => {
    const { rerender } = render(<StatusBadge status="OFFER" />)
    expect(screen.getByText('Offer')).toBeInTheDocument()

    rerender(<StatusBadge status="REJECTED" />)
    expect(screen.getByText('Rejected')).toBeInTheDocument()

    rerender(<StatusBadge status="FINAL_ROUND" />)
    expect(screen.getByText('Final Round')).toBeInTheDocument()
  })
})
