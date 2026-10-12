import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'

/**
 * Mastery is never conveyed by colour alone (US-41: "an accessible
 * non-colour indicator") — the percentage and a plain-language band are
 * always in the label text; colour is secondary reinforcement, the same
 * convention {@code StatusBadge} already established for application
 * status.
 */
function band(score: number): { label: string; className: string } {
  if (score >= 0.7) {
    return { label: 'Strong', className: 'border-transparent bg-success/10 text-success' }
  }
  if (score >= 0.4) {
    return { label: 'Developing', className: 'border-transparent bg-warning/10 text-warning' }
  }
  return { label: 'Needs work', className: 'border-transparent bg-destructive/10 text-destructive' }
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
      <Badge
        variant="outline"
        className={cn('bg-muted text-muted-foreground border-transparent font-medium', className)}
        title={`Only ${observationCount} observation${observationCount === 1 ? '' : 's'} so far — at least 2 are needed before mastery is reported.`}
      >
        Insufficient data
      </Badge>
    )
  }

  const meta = band(masteryScore)
  const percent = Math.round(masteryScore * 100)
  return (
    <Badge
      variant="outline"
      className={cn('font-medium', meta.className, className)}
      title={`${percent}% mastery from ${observationCount} observations`}
    >
      {percent}% · {meta.label}
    </Badge>
  )
}
