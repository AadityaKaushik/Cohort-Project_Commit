import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { FunnelIcon } from 'lucide-react'
import type { Funnel } from '@/lib/analytics-api'
import { ChartEmpty, ChartFrame } from '@/components/shared/Chart'
import { CHART_ANIMATION_MS, SERIES_COLORS, axisProps, gridProps, tooltipProps } from '@/lib/chart-theme'

const STAGE_LABELS = ['Applied', 'OA', 'Interview', 'Final Round', 'Offer'] as const
const HEIGHT = 248

export function FunnelChart({ funnel }: { funnel: Funnel }) {
  if (funnel.sample_size === 0) {
    return <ChartEmpty icon={<FunnelIcon aria-hidden className="size-4" />} message="No applications yet." height={HEIGHT} />
  }

  const data = [
    { stage: STAGE_LABELS[0], count: funnel.applied },
    { stage: STAGE_LABELS[1], count: funnel.reached_oa },
    { stage: STAGE_LABELS[2], count: funnel.reached_interview },
    { stage: STAGE_LABELS[3], count: funnel.reached_final },
    { stage: STAGE_LABELS[4], count: funnel.reached_offer },
  ]

  return (
    <div data-testid="funnel-chart" className="space-y-4">
      <ChartFrame height={HEIGHT} label={`Application funnel: ${data.map((point) => `${point.stage} ${point.count}`).join(', ')}`}>
        {(animate) => (
          <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 640, height: HEIGHT }}>
            <BarChart data={data} layout="vertical" margin={{ left: 0, right: 32, top: 4, bottom: 0 }} barCategoryGap={10}>
              <CartesianGrid {...gridProps} horizontal={false} />
              <XAxis type="number" allowDecimals={false} {...axisProps} />
              <YAxis type="category" dataKey="stage" width={92} {...axisProps} />
              <Tooltip {...tooltipProps} cursor={{ fill: 'var(--color-muted)', opacity: 0.6 }} />
              <Bar
                dataKey="count"
                name="Applications"
                fill={SERIES_COLORS[0]}
                radius={[0, 4, 4, 0]}
                barSize={22}
                isAnimationActive={animate}
                animationDuration={CHART_ANIMATION_MS}
                animationEasing="ease-out"
              >
                <LabelList dataKey="count" position="right" fill="var(--color-foreground)" fontSize={12} fontWeight={600} />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </ChartFrame>
      <dl data-testid="funnel-summary" className="grid grid-cols-3 gap-2 border-t pt-4 text-center sm:grid-cols-5">
        {data.map((point, index) => (
          <div key={point.stage} className="space-y-0.5">
            <dt className="eyebrow">{point.stage}</dt>
            <dd className="text-base font-semibold tabular-nums">{point.count}</dd>
            {index > 0 && data[0].count > 0 && (
              <dd className="text-[11px] text-muted-foreground tabular-nums">{Math.round((point.count / data[0].count) * 100)}% of applied</dd>
            )}
          </div>
        ))}
      </dl>
    </div>
  )
}
