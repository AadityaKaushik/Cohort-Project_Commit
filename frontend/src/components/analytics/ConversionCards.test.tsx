import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { ConversionCards } from '@/components/analytics/ConversionCards'
import type { ConversionRates } from '@/lib/analytics-api'

function sampleRates(overrides: Partial<ConversionRates> = {}): ConversionRates {
  return {
    application_to_oa: { value: 0.6, formatted: '60.0%', numerator: 6, denominator: 10, still_open: 0, sufficient_data: true },
    oa_to_interview: { value: 0.5, formatted: '50.0%', numerator: 3, denominator: 6, still_open: 0, sufficient_data: true },
    interview_to_final: { value: 0.5, formatted: 'insufficient data', numerator: 1, denominator: 2, still_open: 0, sufficient_data: false },
    final_to_offer: { value: 1.0, formatted: 'insufficient data', numerator: 1, denominator: 1, still_open: 0, sufficient_data: false },
    overall_success: { value: 0.1, formatted: '10.0%', numerator: 1, denominator: 10, still_open: 0, sufficient_data: true },
    ...overrides,
  }
}

describe('ConversionCards', () => {
  it('shows every rate label with its formatted value and denominator', () => {
    render(<ConversionCards rates={sampleRates()} />)

    expect(screen.getByText('Applied → OA')).toBeInTheDocument()
    expect(screen.getByText('60.0%')).toBeInTheDocument()
    expect(screen.getByText('6 / 10')).toBeInTheDocument();
  })

  it('de-emphasises a low-confidence rate instead of showing a misleading percentage', () => {
    render(<ConversionCards rates={sampleRates()} />)

    // interview_to_final is insufficient -- the dash placeholder, not its computed rate, is shown.
    const heading = screen.getByText('Interview → Final Round')
    const card = heading.closest('[data-slot="card"]') ?? heading.parentElement!.parentElement!
    expect(within(card as HTMLElement).getByText('1 / 2')).toBeInTheDocument()
    expect(within(card as HTMLElement).getByText('—')).toBeInTheDocument()
    expect(within(card as HTMLElement).queryByText(/%/)).not.toBeInTheDocument()
    expect(within(card as HTMLElement).getByText('Low confidence · n=2')).toBeInTheDocument()
  })
})
