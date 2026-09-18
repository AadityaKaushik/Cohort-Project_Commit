import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: ReactNode
}

/**
 * The one empty-state shape used everywhere (PROJECT_PLAN.md §20.3):
 * icon, one sentence of explanation, one primary action. Used both for
 * "no data yet" placeholders on unbuilt pages and later for genuine
 * empty states once each feature ships.
 */
export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 rounded-lg border border-dashed p-10 text-center">
      <div className="flex h-11 w-11 items-center justify-center rounded-md bg-muted">
        <Icon className="h-5 w-5 text-muted-foreground" />
      </div>
      <div className="space-y-1">
        <h2 className="text-sm font-medium">{title}</h2>
        <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
      </div>
      {action}
    </div>
  )
}
