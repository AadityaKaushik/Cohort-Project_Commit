import type { CategoryProgress } from '@/lib/prep-api'

const CATEGORY_LABELS: Record<CategoryProgress['category'], string> = {
  dsa: 'Data Structures & Algorithms',
  system_design: 'System Design',
  databases: 'Databases',
  os_networking: 'OS & Networking',
  oop: 'OOP',
  behavioural: 'Behavioural',
}

/**
 * US-41: "progress bars per category." The numeric label is always present
 * next to the bar (never colour-only), and the bar itself uses a plain
 * `role="progressbar"` div rather than a chart library — a single filled
 * track needs no more than that.
 */
export function CategoryProgressBar({ progress }: { progress: CategoryProgress }) {
  const completionPercent = progress.tasks_total === 0 ? 0 : Math.round((progress.tasks_completed / progress.tasks_total) * 100)
  const masteryLabel = progress.average_mastery === null ? 'not enough data yet' : `${Math.round(progress.average_mastery * 100)}% avg. mastery`

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium">{CATEGORY_LABELS[progress.category]}</span>
        <span className="text-muted-foreground">
          {progress.tasks_completed}/{progress.tasks_total} tasks · {masteryLabel}
        </span>
      </div>
      <div
        role="progressbar"
        aria-valuenow={completionPercent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${CATEGORY_LABELS[progress.category]} task completion: ${completionPercent}%`}
        className="h-2 w-full overflow-hidden rounded-full bg-muted"
      >
        <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${completionPercent}%` }} />
      </div>
    </div>
  )
}
