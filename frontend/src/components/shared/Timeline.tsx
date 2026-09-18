import { format, formatDistanceToNow } from 'date-fns'
import { HistoryIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'

export interface TimelineEntry {
  id: string
  title: string
  description?: string | null
  timestamp: string
}

/** Newest-first vertical timeline (US-15): each entry shows relative and absolute time. */
export function Timeline({ entries }: { entries: TimelineEntry[] }) {
  if (entries.length === 0) {
    return (
      <EmptyState
        icon={HistoryIcon}
        title="No history yet"
        description="Once this application's status changes, every change will show up here."
      />
    )
  }

  return (
    <ol className="relative space-y-6 border-s border-border ps-6">
      {entries.map((entry) => {
        const date = new Date(entry.timestamp)
        return (
          <li key={entry.id} className="relative">
            <span className="absolute top-1 -start-[29px] h-3 w-3 rounded-full border-2 border-background bg-primary" />
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
