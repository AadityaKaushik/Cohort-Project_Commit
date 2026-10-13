import { Link } from 'react-router-dom'
import type { ApplicationStatusValue, ApplicationSummary } from '@/lib/applications-api'

const PIPELINE_STAGES: { status: ApplicationStatusValue; label: string }[] = [
  { status: 'SAVED', label: 'Saved' },
  { status: 'APPLIED', label: 'Applied' },
  { status: 'OA', label: 'OA' },
  { status: 'INTERVIEW', label: 'Interview' },
  { status: 'FINAL_ROUND', label: 'Final Round' },
  { status: 'OFFER', label: 'Offer' },
]

/**
 * US-49: "a compact pipeline strip with live counts" -- how many
 * applications are sitting at each non-terminal status *right now*
 * (current status, not "ever reached" like the funnel on the analytics
 * page). Each tile links to the applications list (status filtering on
 * that list is not yet built -- see ApplicationsListPage -- so the link
 * targets the list itself rather than a filter query param that would
 * silently do nothing).
 */
export function PipelineStrip({ applications }: { applications: ApplicationSummary[] }) {
  const countsByStatus = new Map<ApplicationStatusValue, number>()
  for (const application of applications) {
    countsByStatus.set(application.status, (countsByStatus.get(application.status) ?? 0) + 1)
  }

  const largest = Math.max(1, ...PIPELINE_STAGES.map(({ status }) => countsByStatus.get(status) ?? 0))

  return (
    <div data-testid="pipeline-strip" className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
      {PIPELINE_STAGES.map(({ status, label }, index) => {
        const count = countsByStatus.get(status) ?? 0
        return (
          <Link
            key={status}
            to="/applications"
            className="lift group/stage relative flex flex-col gap-3 rounded-xl border bg-card p-3.5 hover:border-primary/30"
          >
            <span className="flex items-center justify-between text-xs text-muted-foreground">
              {label}
              <span className="font-mono text-[10px] opacity-60">{String(index + 1).padStart(2, '0')}</span>
            </span>
            <span className="text-3xl font-semibold tracking-tight tabular-nums">{count}</span>
            <span className="h-1 overflow-hidden rounded-full bg-muted" aria-hidden>
              <span
                className={`animate-grow-x block h-full origin-left rounded-full ${status === 'OFFER' ? 'bg-success' : 'bg-primary'}`}
                style={{ width: `${(count / largest) * 100}%`, animationDelay: `${index * 60}ms` }}
              />
            </span>
          </Link>
        )
      })}
    </div>
  )
}
