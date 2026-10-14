import { format, formatDistanceToNow } from 'date-fns'
import { HistoryIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'
import { cn } from '@/lib/utils'

export interface TimelineEntry {
  id: string
  title: string
  description?: string | null
  timestamp: string
}

export function Timeline({ entries }: { entries: TimelineEntry[] }) {
  if (entries.length === 0) {
    return (
      <EmptyState
        compact
        icon={HistoryIcon}
        title="No history yet"
        description="Once this application's status changes, every change will show up here."
      />
    )
  }

  return (
    <ol className="relative space-y-5 ps-7 before:absolute before:inset-y-1.5 before:start-[7px] before:w-px before:bg-border">
      {entries.map((entry, index) => {
        const date = new Date(entry.timestamp)
        return (
          <li key={entry.id} className="relative">
            <span
              aria-hidden
              className={cn('absolute top-0.5 -start-7 flex size-[15px] items-center justify-center rounded-full ring-4 ring-card', index === 0 ? 'bg-primary' : 'bg-muted')}
            >
              <span className={cn('size-1.5 rounded-full', index === 0 ? 'bg-primary-foreground' : 'bg-muted-foreground')} />
            </span>
            <p className="text-sm font-medium">{entry.title}</p>
            {entry.description && <p className="mt-0.5 text-sm text-muted-foreground">{entry.description}</p>}
            <p className="mt-1 text-xs text-muted-foreground" title={format(date, 'PPpp')}>
              {formatDistanceToNow(date, { addSuffix: true })}
            </p>
          </li>
        )
      })}
    </ol>
  )
}
