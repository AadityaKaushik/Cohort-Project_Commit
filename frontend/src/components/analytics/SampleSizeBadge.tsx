import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'

/**
 * US-50: "a shared low-confidence badge component is used consistently"
 * across every analytics number in the app. Sample size is always shown as
 * plain text -- never colour-only (same accessibility convention as
 * {@code MasteryBadge}) -- and the insufficient-data styling matches it too,
 * so a user learns one visual language for "not enough data yet" everywhere.
 */
interface SampleSizeBadgeProps {
  sampleSize: number
  sufficientData: boolean
  className?: string
}

export function SampleSizeBadge({ sampleSize, sufficientData, className }: SampleSizeBadgeProps) {
  if (!sufficientData) {
    return (
      <Badge
        variant="outline"
        className={cn('bg-muted text-muted-foreground border-transparent font-medium', className)}
        title={`Only ${sampleSize} observation${sampleSize === 1 ? '' : 's'} so far — at least 5 are needed for a confident number.`}
      >
        Low confidence · n={sampleSize}
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className={cn('bg-emerald-100 text-emerald-800 border-transparent font-medium dark:bg-emerald-950 dark:text-emerald-300', className)}
      title={`Based on ${sampleSize} observations.`}
    >
      n={sampleSize}
    </Badge>
  )
}
