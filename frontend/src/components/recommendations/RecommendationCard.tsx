import { useState, type FormEvent } from 'react'
import { CheckIcon, ClockIcon, TrendingDownIcon, TrendingUpIcon, XIcon } from 'lucide-react'
import type { Recommendation } from '@/lib/recommendations-api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const TIER_LABEL: Record<Recommendation['tier'], string> = {
  A: 'Tier A — your data',
  B: 'Tier B — thin evidence',
  C: 'Tier C — federated',
  D: 'Tier D — heuristic',
}

const TIER_BADGE_VARIANT: Record<Recommendation['tier'], 'default' | 'secondary' | 'outline'> = {
  A: 'default',
  B: 'secondary',
  C: 'secondary',
  D: 'outline',
}

interface RecommendationCardProps {
  recommendation: Recommendation
  onAccept: (id: string) => void
  onSnooze: (id: string) => void
  onDismiss: (id: string, reason: string) => void
  isBusy?: boolean
  /** US-65: shows this recommendation's rank_delta badge when the Federated Lift toggle is on. */
  showRankDelta?: boolean
}

/** US-51..56,65: one recommendation, its evidence tier, the observations behind it, the accept/snooze/dismiss actions, and (when toggled) its federated rank delta. */
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
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-2 space-y-0">
        <CardTitle className="text-base font-medium">{recommendation.headline}</CardTitle>
        <div className="flex items-center gap-2">
          {showRankDelta && recommendation.rank_delta !== 0 && (
            <Badge
              variant="outline"
              className={recommendation.rank_delta > 0 ? 'text-emerald-600' : 'text-amber-600'}
              title="Position change vs. local-only ranking"
            >
              {recommendation.rank_delta > 0 ? (
                <TrendingUpIcon className="h-3 w-3" />
              ) : (
                <TrendingDownIcon className="h-3 w-3" />
              )}
              {recommendation.rank_delta > 0 ? `+${recommendation.rank_delta}` : recommendation.rank_delta}
            </Badge>
          )}
          <Badge variant={TIER_BADGE_VARIANT[recommendation.tier]} title={TIER_LABEL[recommendation.tier]}>
            {TIER_LABEL[recommendation.tier]}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <p className="text-sm text-muted-foreground">{recommendation.explanation}</p>

        {recommendation.evidence.length > 0 && (
          <ul className="space-y-1 rounded-md border border-dashed p-2 text-xs text-muted-foreground">
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

        {!dismissing && (
          <div className="flex flex-wrap gap-2">
            <Button size="sm" disabled={isBusy} onClick={() => onAccept(recommendation.id)}>
              <CheckIcon className="h-4 w-4" />
              Accept
            </Button>
            <Button size="sm" variant="outline" disabled={isBusy} onClick={() => onSnooze(recommendation.id)}>
              <ClockIcon className="h-4 w-4" />
              Snooze 7 days
            </Button>
            <Button size="sm" variant="ghost" disabled={isBusy} onClick={() => setDismissing(true)}>
              <XIcon className="h-4 w-4" />
              Dismiss
            </Button>
          </div>
        )}

        {dismissing && (
          <form onSubmit={handleDismissSubmit} className="flex flex-col gap-2 sm:flex-row sm:items-end">
            <div className="flex-1 space-y-1">
              <Label htmlFor={`dismiss-reason-${recommendation.id}`}>Why dismiss this?</Label>
              <Input
                id={`dismiss-reason-${recommendation.id}`}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="e.g. Not relevant right now"
                autoFocus
              />
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
      </CardContent>
    </Card>
  )
}
