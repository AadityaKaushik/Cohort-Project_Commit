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
    return { label: 'Strong', className: 'bg-emerald-100 text-emerald-800 border-transparent dark:bg-emerald-950 dark:text-emerald-300' }
  }
  if (score >= 0.4) {
    return { label: 'Developing', className: 'bg-amber-100 text-amber-800 border-transparent dark:bg-amber-950 dark:text-amber-300' }
  }
  return { label: 'Needs work', className: 'bg-red-100 text-red-800 border-transparent dark:bg-red-950 dark:text-red-300' }
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
