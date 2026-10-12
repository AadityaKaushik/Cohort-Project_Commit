import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ActivityIcon } from 'lucide-react'
import { format, parseISO } from 'date-fns'
import type { Timeline } from '@/lib/analytics-api'

/**
 * US-45: applications and interviews plotted over time, weekly buckets, as
 * two series. Two series need a legend and identity-by-colour beyond just
 * position (dataviz's categorical rule) -- `--color-chart-1`/`--color-chart-2`
 * are assigned in a fixed order (applications always chart-1, interviews
 * always chart-2) and never reassigned based on which series has more data,
 * so colour tracks the entity, not its rank.
 */
export function ActivityTimelineChart({ timeline }: { timeline: Timeline }) {
  if (timeline.sample_size === 0) {
    return (
      <div className="flex min-h-[220px] flex-col items-center justify-center gap-2 rounded-md border border-dashed text-center text-sm text-muted-foreground">
        <ActivityIcon className="h-5 w-5" />
        <p>No activity in this range yet — applications and interviews you log will show up here.</p>
      </div>
    )
  }

  const data = timeline.buckets.map((bucket) => ({
    week: format(parseISO(bucket.week_start), 'MMM d'),
    applications: bucket.applications,
    interviews: bucket.interviews,
  }))

  return (
    <div data-testid="activity-timeline-chart" className="h-[280px] w-full">
      <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 640, height: 280 }}>
      <LineChart data={data} margin={{ left: 0, right: 16, top: 8 }}>
        <CartesianGrid vertical={false} stroke="var(--color-border)" />
        <XAxis dataKey="week" stroke="var(--color-muted-foreground)" fontSize={12} />
        <YAxis allowDecimals={false} stroke="var(--color-muted-foreground)" fontSize={12} />
        <Tooltip
          contentStyle={{ background: 'var(--color-card)', border: '1px solid var(--color-border)', borderRadius: 8 }}
          labelStyle={{ color: 'var(--color-foreground)' }}
        />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
        <Line type="monotone" dataKey="applications" name="Applications" stroke="var(--color-chart-1)" strokeWidth={2} dot={false} />
        <Line type="monotone" dataKey="interviews" name="Interviews" stroke="var(--color-chart-2)" strokeWidth={2} dot={false} />
      </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
