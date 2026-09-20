import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ActivityTimelineChart } from '@/components/analytics/ActivityTimelineChart'
import type { Timeline } from '@/lib/analytics-api'

describe('ActivityTimelineChart', () => {
  it('renders the empty state when there is no activity in range', () => {
    render(<ActivityTimelineChart timeline={{ buckets: [], sample_size: 0 }} />)

    expect(screen.getByText(/no activity in this range yet/i)).toBeInTheDocument()
    expect(screen.queryByTestId('activity-timeline-chart')).not.toBeInTheDocument()
  })

  it('renders the two-series chart with a legend once there is activity', () => {
    const timeline: Timeline = {
      sample_size: 4,
      buckets: [
        { week_start: '2026-01-05', applications: 3, interviews: 1 },
        { week_start: '2026-01-12', applications: 0, interviews: 0 },
      ],
    }
    render(<ActivityTimelineChart timeline={timeline} />)

    expect(screen.getByTestId('activity-timeline-chart')).toBeInTheDocument()
    expect(screen.getByText('Applications')).toBeInTheDocument()
    expect(screen.getByText('Interviews')).toBeInTheDocument()
  })
})
