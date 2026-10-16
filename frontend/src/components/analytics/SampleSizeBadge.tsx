import { ToneBadge } from '@/components/shared/ToneBadge'

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
        title={`Only ${sampleSize} observation${sampleSize === 1 ? '' : 's'}. At least 5 are needed.`}
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
