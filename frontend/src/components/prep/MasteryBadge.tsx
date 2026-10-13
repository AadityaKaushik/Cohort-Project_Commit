import { ToneBadge, type Tone } from '@/components/shared/ToneBadge'

/**
 * Mastery is never conveyed by colour alone (US-41: "an accessible
 * non-colour indicator") — the percentage and a plain-language band are
 * always in the label text; colour is secondary reinforcement, the same
 * convention {@code StatusBadge} already established for application
 * status.
 */
function band(score: number): { label: string; tone: Tone } {
  if (score >= 0.7) {
    return { label: 'Strong', tone: 'success' }
  }
  if (score >= 0.4) {
    return { label: 'Developing', tone: 'warning' }
  }
  return { label: 'Needs work', tone: 'danger' }
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
      <ToneBadge
        tone="neutral"
        className={className}
        title={`Only ${observationCount} observation${observationCount === 1 ? '' : 's'} so far — at least 2 are needed before mastery is reported.`}
      >
        Insufficient data
      </ToneBadge>
    )
  }

  const meta = band(masteryScore)
  const percent = Math.round(masteryScore * 100)
  return (
    <ToneBadge
      tone={meta.tone}
      className={className}
      title={`${percent}% mastery from ${observationCount} observations`}
    >
      {percent}% · {meta.label}
    </ToneBadge>
  )
}
