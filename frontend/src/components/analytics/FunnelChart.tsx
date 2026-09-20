import { Bar, BarChart, CartesianGrid, LabelList, Tooltip, XAxis, YAxis } from 'recharts'
import { FunnelIcon } from 'lucide-react'
import type { Funnel } from '@/lib/analytics-api'

const STAGE_LABELS = ['Applied', 'OA', 'Interview', 'Final Round', 'Offer'] as const

/**
 * US-43: the application funnel, one bar per stage, each count "ever
 * reached" (not current status). A single series (one bar chart, one
 * measure -- "how many applications reached this stage") needs only one
 * consistent hue, not a categorical palette -- the five stage names are the
 * chart's categorical axis, not five different series, so there is no
 * identity-by-colour requirement here. Every bar carries its count as a
 * direct label (five bars is few enough that this reads cleanly without
 * clutter), and a tooltip on hover repeats it for anyone zoomed in or using
 * a screen magnifier.
 */
export function FunnelChart({ funnel }: { funnel: Funnel }) {
  if (funnel.sample_size === 0) {
    return (
      <div className="flex min-h-[220px] flex-col items-center justify-center gap-2 rounded-md border border-dashed text-center text-sm text-muted-foreground">
        <FunnelIcon className="h-5 w-5" />
        <p>No applications yet — once you log one, your funnel appears here.</p>
      </div>
    )
  }

  const data = [
    { stage: STAGE_LABELS[0], count: funnel.applied },
    { stage: STAGE_LABELS[1], count: funnel.reached_oa },
    { stage: STAGE_LABELS[2], count: funnel.reached_interview },
    { stage: STAGE_LABELS[3], count: funnel.reached_final },
    { stage: STAGE_LABELS[4], count: funnel.reached_offer },
  ]

  return (
    <div data-testid="funnel-chart">
      <div className="h-[260px] w-full overflow-x-auto">
        <BarChart width={640} height={260} data={data} layout="vertical" margin={{ left: 16, right: 24 }}>
          <CartesianGrid horizontal={false} stroke="var(--color-border)" />
          <XAxis type="number" allowDecimals={false} stroke="var(--color-muted-foreground)" fontSize={12} />
          <YAxis type="category" dataKey="stage" width={90} stroke="var(--color-muted-foreground)" fontSize={12} />
          <Tooltip
            cursor={{ fill: 'var(--color-muted)' }}
            contentStyle={{ background: 'var(--color-card)', border: '1px solid var(--color-border)', borderRadius: 8 }}
            labelStyle={{ color: 'var(--color-foreground)' }}
          />
          <Bar dataKey="count" fill="var(--color-chart-1)" radius={4} barSize={22}>
            <LabelList dataKey="count" position="right" fill="var(--color-foreground)" fontSize={12} />
          </Bar>
        </BarChart>
      </div>
      {/* A plain-text equivalent of the chart above -- never colour- or
          hover-only, and robust for screen readers/low vision the same way
          MasteryBadge's plain-language labels are (US-41's own precedent). */}
      <dl data-testid="funnel-summary" className="mt-3 grid grid-cols-5 gap-2 text-center text-xs">
        {data.map((point) => (
          <div key={point.stage}>
            <dt className="text-muted-foreground">{point.stage}</dt>
            <dd className="text-sm font-semibold">{point.count}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
