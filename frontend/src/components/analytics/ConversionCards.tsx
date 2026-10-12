import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'
import type { ConversionRate, ConversionRates } from '@/lib/analytics-api'

const RATE_LABELS: { key: keyof ConversionRates; label: string }[] = [
  { key: 'application_to_oa', label: 'Applied → OA' },
  { key: 'oa_to_interview', label: 'OA → Interview' },
  { key: 'interview_to_final', label: 'Interview → Final Round' },
  { key: 'final_to_offer', label: 'Final Round → Offer' },
  { key: 'overall_success', label: 'Overall (Applied → Offer)' },
]

/**
 * US-44 / FR-67: one card per §20.7 conversion rate, each showing its own
 * value, denominator, and a {@link SampleSizeBadge} -- "every rate displays
 * its denominator" and low-confidence rates are "visually de-emphasised"
 * (muted text) rather than hidden outright, so the number is still there
 * for anyone who wants it, just clearly flagged.
 */
export function ConversionCards({ rates }: { rates: ConversionRates }) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {RATE_LABELS.map(({ key, label }) => (
        <ConversionCard key={key} label={label} rate={rates[key]} />
      ))}
    </div>
  )
}

function ConversionCard({ label, rate }: { label: string; rate: ConversionRate }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <p className={rate.sufficient_data ? 'text-2xl font-semibold' : 'text-2xl font-semibold text-muted-foreground'}>
          {rate.sufficient_data ? rate.formatted : '—'}
        </p>
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>
            {rate.numerator} / {rate.denominator}
          </span>
          <SampleSizeBadge sampleSize={rate.denominator} sufficientData={rate.sufficient_data} />
        </div>
        <p className="text-xs text-muted-foreground">
          {rate.numerator} moved on, {rate.denominator - rate.numerator} did not
          {rate.still_open > 0 ? `, ${rate.still_open} still waiting` : ''}
        </p>
      </CardContent>
    </Card>
  )
}
