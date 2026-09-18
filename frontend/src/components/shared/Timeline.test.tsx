import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Timeline } from '@/components/shared/Timeline'

describe('Timeline', () => {
  it('shows an empty state when there is no history yet', () => {
    render(<Timeline entries={[]} />)
    expect(screen.getByText('No history yet')).toBeInTheDocument()
  })

  it('renders entries newest-first as given, with title and optional description', () => {
    render(
      <Timeline
        entries={[
          { id: '2', title: 'APPLIED → OA', description: null, timestamp: '2026-01-02T00:00:00Z' },
          { id: '1', title: 'Created as APPLIED', description: 'initial note', timestamp: '2026-01-01T00:00:00Z' },
        ]}
      />,
    )

    expect(screen.getByText('APPLIED → OA')).toBeInTheDocument()
    expect(screen.getByText('Created as APPLIED')).toBeInTheDocument()
    expect(screen.getByText('initial note')).toBeInTheDocument()
  })
})
