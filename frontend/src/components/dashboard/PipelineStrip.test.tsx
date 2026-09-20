import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { PipelineStrip } from '@/components/dashboard/PipelineStrip'
import type { ApplicationSummary } from '@/lib/applications-api'

function application(overrides: Partial<ApplicationSummary> = {}): ApplicationSummary {
  return {
    id: 'app-1',
    company: { id: 'company-1', name: 'Acme', website: null, industry: null },
    job_title: 'Engineer',
    status: 'APPLIED',
    current_stage: 'APPLIED',
    days_in_stage: 1,
    date_applied: '2026-01-01',
    priority: null,
    archived: false,
    ...overrides,
  }
}

describe('PipelineStrip', () => {
  it('renders a tile per pipeline stage with live counts by current status', () => {
    render(
      <MemoryRouter>
        <PipelineStrip
          applications={[
            application({ id: '1', status: 'APPLIED' }),
            application({ id: '2', status: 'APPLIED' }),
            application({ id: '3', status: 'OA' }),
            application({ id: '4', status: 'REJECTED' }), // terminal -- not part of the pipeline strip
          ]}
        />
      </MemoryRouter>,
    )

    expect(screen.getByText('Applied')).toBeInTheDocument()
    expect(screen.getByText('OA')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('shows zero for a stage with no current applications', () => {
    render(
      <MemoryRouter>
        <PipelineStrip applications={[]} />
      </MemoryRouter>,
    )

    const zeros = screen.getAllByText('0')
    expect(zeros.length).toBe(6)
  })
})
