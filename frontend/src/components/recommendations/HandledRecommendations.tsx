import { Link } from 'react-router-dom'
import { format } from 'date-fns'
import type { Recommendation } from '@/lib/recommendations-api'
import { formatLabel } from '@/lib/labels'
import { ToneBadge, type Tone } from '@/components/shared/ToneBadge'
import { Button } from '@/components/ui/button'

interface HandledRecommendationsProps {
  items: Recommendation[]
  isBusy: boolean
  onRestore: (id: string) => void
}

const STATUS_TONES: Partial<Record<Recommendation['status'], Tone>> = { accepted: 'success', snoozed: 'warning', dismissed: 'neutral' }

function describe(item: Recommendation): string {
  const until = item.suppressed_until ? format(new Date(item.suppressed_until), 'MMM d') : null
  switch (item.status) {
    case 'accepted':
      return item.accepted_task_id ? 'Added to your preparation plan.' : 'Marked as handled.'
    case 'snoozed':
      return until ? `Hidden until ${until}.` : 'Snoozed.'
    case 'dismissed':
      return `Hides every ${formatLabel(item.type).toLowerCase()} recommendation${until ? ` until ${until}` : ''}.${
        item.dismissed_reason ? ` Reason: ${item.dismissed_reason}` : ''
      }`
    default:
      return ''
  }
}

export function HandledRecommendations({ items, isBusy, onRestore }: HandledRecommendationsProps) {
  return (
    <ul className="surface-raised divide-y overflow-hidden rounded-xl border">
      {items.map((item) => (
        <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm transition-colors hover:bg-accent/40">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-medium">{item.headline}</span>
              <ToneBadge tone={STATUS_TONES[item.status] ?? 'neutral'}>{formatLabel(item.status)}</ToneBadge>
            </div>
            <p className="mt-1 text-muted-foreground">{describe(item)}</p>
          </div>
          <div className="flex gap-2">
            {item.status === 'accepted' && item.accepted_task_id && (
              <Button variant="outline" size="sm" asChild>
                <Link to="/prep">See in plan</Link>
              </Button>
            )}
            {item.status === 'accepted' && !item.accepted_task_id && item.target_application_id && (
              <Button variant="outline" size="sm" asChild>
                <Link to={`/applications/${item.target_application_id}`}>Open application</Link>
              </Button>
            )}
            {(item.status === 'snoozed' || item.status === 'dismissed') && (
              <Button variant="outline" size="sm" disabled={isBusy} onClick={() => onRestore(item.id)}>
                Bring back
              </Button>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
