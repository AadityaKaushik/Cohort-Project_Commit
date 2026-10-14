import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'
import type { ConversionRate, ConversionRates } from '@/lib/analytics-api'
import { cn } from '@/lib/utils'

const RATE_LABELS: { key: keyof ConversionRates; label: string }[] = [
  { key: 'application_to_oa', label: 'Applied → OA' },
  { key: 'oa_to_interview', label: 'OA → Interview' },
  { key: 'interview_to_final', label: 'Interview → Final Round' },
  { key: 'final_to_offer', label: 'Final Round → Offer' },
  { key: 'overall_success', label: 'Overall (Applied → Offer)' },
]

export function ConversionCards({ rates }: { rates: ConversionRates }) {
  return (
    <div className="stagger grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
      {RATE_LABELS.map(({ key, label }) => (
        <ConversionCard key={key} label={label} rate={rates[key]} />
      ))}
    </div>
  )
}

function ConversionCard({ label, rate }: { label: string; rate: ConversionRate }) {
  const percent = rate.sufficient_data ? Math.round(rate.value * 100) : 0
  return (
    <div className="surface-raised flex flex-col gap-3 rounded-xl border p-4">
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
      </div>
      <p className={cn('text-2xl font-semibold tracking-tight tabular-nums', !rate.sufficient_data && 'text-muted-foreground')}>
        {rate.sufficient_data ? rate.formatted : '—'}
      </p>
      <div aria-hidden className="h-1 overflow-hidden rounded-full bg-muted">
        <span className="animate-grow-x block h-full origin-left rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
        <span className="tabular-nums">
          {rate.numerator} / {rate.denominator}
        </span>
        <SampleSizeBadge sampleSize={rate.denominator} sufficientData={rate.sufficient_data} />
      </div>
      <p className="text-xs text-pretty text-muted-foreground">
        {rate.numerator} moved on, {rate.denominator - rate.numerator} did not
        {rate.still_open > 0 ? `, ${rate.still_open} still waiting` : ''}
      </p>
    </div>
  )
}
