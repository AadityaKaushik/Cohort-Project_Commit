import { ToneBadge } from '@/components/shared/ToneBadge'

/**
 * US-50: "a shared low-confidence badge component is used consistently"
 * across every analytics number in the app. Sample size is always shown as
 * plain text, never colour-only, and the insufficient-data styling matches
 * MasteryBadge so "not enough data yet" reads the same everywhere.
 */
interface SampleSizeBadgeProps {
  sampleSize: number
  sufficientData: boolean
  className?: string
}

export function SampleSizeBadge({ sampleSize, sufficientData, className }: SampleSizeBadgeProps) {
  if (!sufficientData) {
    return (
      <ToneBadge
        tone="neutral"
        className={className}
        title={`Only ${sampleSize} observation${sampleSize === 1 ? '' : 's'} so far — at least 5 are needed for a confident number.`}
      >
        Low confidence · n={sampleSize}
      </ToneBadge>
    )
  }

  return (
    <ToneBadge tone="success" className={className} title={`Based on ${sampleSize} observations.`}>
      n={sampleSize}
    </ToneBadge>
  )
}
