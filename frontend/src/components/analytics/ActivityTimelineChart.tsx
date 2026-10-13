import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ActivityIcon } from 'lucide-react'
import { format, parseISO } from 'date-fns'
import type { Timeline } from '@/lib/analytics-api'
import { ChartEmpty, ChartFrame, ChartLegend } from '@/components/shared/Chart'
import { CHART_ANIMATION_MS, SERIES_COLORS, axisProps, gridProps, tooltipProps } from '@/lib/chart-theme'

const HEIGHT = 260

const SERIES = [
  { key: 'applications', label: 'Applications', color: SERIES_COLORS[0] },
  { key: 'interviews', label: 'Interviews', color: SERIES_COLORS[1] },
] as const

/**
 * US-45: applications and interviews plotted over time, weekly buckets, as
 * two series. Colour tracks the entity, never its rank: applications are
 * always series 1 and interviews always series 2, and a legend names both.
 */
export function ActivityTimelineChart({ timeline }: { timeline: Timeline }) {
  if (timeline.sample_size === 0) {
    return (
      <ChartEmpty
        icon={<ActivityIcon aria-hidden className="size-4" />}
        message="No activity in this range yet — applications and interviews you log will show up here."
        height={HEIGHT}
      />
    )
  }

  const data = timeline.buckets.map((bucket) => ({
    week: format(parseISO(bucket.week_start), 'MMM d'),
    applications: bucket.applications,
    interviews: bucket.interviews,
  }))
  const totals = SERIES.map((series) => `${series.label} ${data.reduce((sum, point) => sum + point[series.key], 0)}`).join(', ')

  return (
    <div className="space-y-3">
      <ChartLegend items={SERIES.map(({ label, color }) => ({ label, color }))} />
      <ChartFrame height={HEIGHT} testId="activity-timeline-chart" label={`Weekly activity over ${data.length} weeks: ${totals}`}>
        {(animate) => (
          <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 640, height: HEIGHT }}>
            <AreaChart data={data} margin={{ left: -16, right: 8, top: 8, bottom: 0 }}>
              <defs>
                {SERIES.map((series) => (
                  <linearGradient key={series.key} id={`fill-${series.key}`} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={series.color} stopOpacity={0.18} />
                    <stop offset="100%" stopColor={series.color} stopOpacity={0} />
                  </linearGradient>
                ))}
              </defs>
              <CartesianGrid {...gridProps} vertical={false} />
              <XAxis dataKey="week" {...axisProps} minTickGap={24} />
              <YAxis allowDecimals={false} {...axisProps} />
              <Tooltip {...tooltipProps} cursor={{ stroke: 'var(--color-muted-foreground)', strokeDasharray: '3 3' }} />
              {SERIES.map((series) => (
                <Area
                  key={series.key}
                  type="monotone"
                  dataKey={series.key}
                  name={series.label}
                  stroke={series.color}
                  strokeWidth={2}
                  fill={`url(#fill-${series.key})`}
                  dot={false}
                  activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--color-card)' }}
                  isAnimationActive={animate}
                  animationDuration={CHART_ANIMATION_MS}
                  animationEasing="ease-out"
                />
              ))}
            </AreaChart>
          </ResponsiveContainer>
        )}
      </ChartFrame>
    </div>
  )
}
