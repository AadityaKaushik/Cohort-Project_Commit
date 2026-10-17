import { band, type ReadinessLevel } from '@/lib/readiness'
import { cn } from '@/lib/utils'

/** Three steps, filled up to the readiness level; a strong topic reaches the top step in apricot. */
export function ReadinessSteps({ level, className }: { level: ReadinessLevel; className?: string }) {
  return (
    <span aria-hidden className={cn('inline-flex h-3.5 items-end gap-[2px]', className)}>
      {[1, 2, 3].map((step) => (
        <span
          key={step}
          className={cn(
            'w-[5px] rounded-[1px]',
            step > level ? 'bg-foreground/15' : level === 3 ? 'bg-line-offer' : 'bg-foreground',
          )}
          style={{ height: `${step * 33.4}%` }}
        />
      ))}
    </span>
  )
}

interface MasteryBadgeProps {
  masteryScore: number
  sufficientEvidence: boolean
  observationCount: number
  className?: string
}

export function MasteryBadge({ masteryScore, sufficientEvidence, observationCount, className }: MasteryBadgeProps) {
  if (!sufficientEvidence) {
    return (
      <span
        className={cn('inline-flex items-center gap-1.5 text-xs text-muted-foreground', className)}
        title={`Only ${observationCount} observation${observationCount === 1 ? '' : 's'}. At least 2 are needed.`}
      >
        <ReadinessSteps level={0} />
        Insufficient data
      </span>
    )
  }

  const meta = band(masteryScore)
  const percent = Math.round(masteryScore * 100)
  return (
    <span
      className={cn('inline-flex items-center gap-1.5 text-xs font-medium', className)}
      title={`${percent}% mastery from ${observationCount} observations`}
    >
      <ReadinessSteps level={meta.level} />
      {percent}% · {meta.label}
    </span>
  )
}
