import { useState, type FormEvent } from 'react'
import { CheckIcon, ClockIcon, TrendingDownIcon, TrendingUpIcon, XIcon } from 'lucide-react'
import type { Recommendation } from '@/lib/recommendations-api'
import { formatLabel } from '@/lib/labels'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/shared/Form'
import { ToneBadge, type Tone } from '@/components/shared/ToneBadge'

const TIER_LABEL: Record<Recommendation['tier'], string> = {
  A: 'Tier A: your data',
  B: 'Tier B: thin evidence',
  C: 'Tier C: federated',
  D: 'Tier D: heuristic',
}

const TIER_TONE: Record<Recommendation['tier'], Tone> = {
  A: 'primary',
  B: 'info',
  C: 'info',
  D: 'neutral',
}

interface RecommendationCardProps {
  recommendation: Recommendation
  onAccept: (id: string) => void
  onSnooze: (id: string) => void
  onDismiss: (id: string, reason: string) => void
  isBusy?: boolean
  showRankDelta?: boolean
}

export function RecommendationCard({
  recommendation,
  onAccept,
  onSnooze,
  onDismiss,
  isBusy = false,
  showRankDelta = false,
}: RecommendationCardProps) {
  const [dismissing, setDismissing] = useState(false)
  const [reason, setReason] = useState('')

  function handleDismissSubmit(event: FormEvent) {
    event.preventDefault()
    if (!reason.trim()) {
      return
    }
    onDismiss(recommendation.id, reason.trim())
    setDismissing(false)
    setReason('')
  }

  return (
    <article className="surface-raised overflow-hidden rounded-xl border transition-colors hover:border-primary/25">
      <div className="flex flex-col gap-3 p-4 sm:p-5">
        <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
          <h3 className="font-heading text-base font-semibold tracking-tight">{recommendation.headline}</h3>
          <div className="flex items-center gap-2">
            {showRankDelta && recommendation.rank_delta !== 0 && (
              <ToneBadge tone={recommendation.rank_delta > 0 ? 'success' : 'warning'} title="Position change vs. local-only ranking">
                {recommendation.rank_delta > 0 ? <TrendingUpIcon aria-hidden /> : <TrendingDownIcon aria-hidden />}
                {recommendation.rank_delta > 0 ? `+${recommendation.rank_delta}` : recommendation.rank_delta}
              </ToneBadge>
            )}
            <ToneBadge tone={TIER_TONE[recommendation.tier]} title={TIER_LABEL[recommendation.tier]}>
              {TIER_LABEL[recommendation.tier]}
            </ToneBadge>
          </div>
        </div>
        <p className="text-sm text-pretty text-muted-foreground">{recommendation.explanation}</p>

        {recommendation.evidence.length > 0 && (
          <ul className="space-y-1.5 rounded-lg border-l-2 border-primary/40 bg-surface-2/60 px-3 py-2.5 text-xs text-muted-foreground">
            {recommendation.evidence.map((item, index) => (
              <li key={index}>
                <span className="font-medium text-foreground">{item.label}:</span> {item.detail}
              </li>
            ))}
          </ul>
        )}

        <p className="text-xs text-muted-foreground">
          Based on {recommendation.observation_count} observation{recommendation.observation_count === 1 ? '' : 's'}.
        </p>
      </div>

      <div className="border-t bg-surface-2/50 px-4 py-3 sm:px-5">
        {!dismissing && (
          <div className="flex flex-wrap gap-2">
            <Button size="sm" disabled={isBusy} onClick={() => onAccept(recommendation.id)}>
              <CheckIcon aria-hidden />
              {recommendation.target_topic_id ? 'Accept and add to my plan' : 'Accept'}
            </Button>
            <Button size="sm" variant="outline" disabled={isBusy} onClick={() => onSnooze(recommendation.id)}>
              <ClockIcon aria-hidden />
              Snooze 7 days
            </Button>
            <Button size="sm" variant="ghost" disabled={isBusy} onClick={() => setDismissing(true)}>
              <XIcon aria-hidden />
              Dismiss
            </Button>
          </div>
        )}

        {dismissing && (
          <form onSubmit={handleDismissSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1">
              <FormField
                id={`dismiss-reason-${recommendation.id}`}
                label="Why dismiss this?"
                hint={`This hides every ${formatLabel(recommendation.type).toLowerCase()} recommendation for 30 days, not just this one. You can bring it back from Handled.`}
              >
                <Input
                  id={`dismiss-reason-${recommendation.id}`}
                  className="bg-card"
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="e.g. Not relevant right now"
                  autoFocus
                />
              </FormField>
            </div>
            <div className="flex gap-2">
              <Button type="submit" size="sm" disabled={isBusy || !reason.trim()}>
                Confirm dismiss
              </Button>
              <Button type="button" size="sm" variant="ghost" onClick={() => setDismissing(false)}>
                Cancel
              </Button>
            </div>
          </form>
        )}
      </div>
    </article>
  )
}
