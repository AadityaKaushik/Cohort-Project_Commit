import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { FunnelChart } from '@/components/analytics/FunnelChart'
import type { Funnel } from '@/lib/analytics-api'

function sampleFunnel(overrides: Partial<Funnel> = {}): Funnel {
  return {
    applied: 10,
    reached_oa: 6,
    reached_interview: 4,
    reached_final: 2,
    reached_offer: 1,
    sample_size: 10,
    ...overrides,
  }
}

describe('FunnelChart', () => {
  it('renders the empty state when there is no data yet', () => {
    render(<FunnelChart funnel={sampleFunnel({ applied: 0, reached_oa: 0, reached_interview: 0, reached_final: 0, reached_offer: 0, sample_size: 0 })} />)

    expect(screen.getByText(/no applications yet/i)).toBeInTheDocument()
    expect(screen.queryByTestId('funnel-chart')).not.toBeInTheDocument()
  })

  it('renders a bar per stage with its count once there is data', () => {
    render(<FunnelChart funnel={sampleFunnel()} />)

    const chart = screen.getByTestId('funnel-chart')
    expect(chart).toBeInTheDocument()
    expect(screen.queryByText(/no applications yet/i)).not.toBeInTheDocument()
    const summary = within(screen.getByTestId('funnel-summary'))
    expect(summary.getByText('Applied')).toBeInTheDocument()
    expect(summary.getByText('Offer')).toBeInTheDocument()
    ;['10', '6', '4', '2', '1'].forEach((count) => {
      expect(summary.getByText(count)).toBeInTheDocument()
    })
  })
})
