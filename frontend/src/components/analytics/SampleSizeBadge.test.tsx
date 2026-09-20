import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'

describe('SampleSizeBadge', () => {
  it('shows the plain sample size when the data is sufficient', () => {
    render(<SampleSizeBadge sampleSize={12} sufficientData />)
    expect(screen.getByText('n=12')).toBeInTheDocument()
  })

  it('flags low confidence, in text, when the data is insufficient', () => {
    render(<SampleSizeBadge sampleSize={2} sufficientData={false} />)
    expect(screen.getByText('Low confidence · n=2')).toBeInTheDocument()
  })
})
