import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { FlDemoStatus } from '@/lib/fl-demo-api'
import { ChartFrame } from '@/components/shared/Chart'
import { axisProps, gridProps, tooltipProps } from '@/lib/chart-theme'

const HEIGHT = 240

/** Check-set accuracy per published model version, against a coin flip and the all-data-in-one-place ceiling. */
export function FlAccuracyChart({ versions, ceiling }: { versions: FlDemoStatus['versions']; ceiling: number | null }) {
  const data = versions
    .filter((version) => version.check_accuracy !== null)
    .map((version) => ({ version: `v${version.version}`, accuracy: Math.round((version.check_accuracy ?? 0) * 1000) / 10 }))
  const latest = data.at(-1)

  return (
    <ChartFrame
      height={HEIGHT}
      label={`Check set accuracy over ${data.length} model versions${latest ? `, now ${latest.accuracy}%` : ''}`}
    >
      <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 560, height: HEIGHT }}>
        <LineChart data={data} margin={{ left: -12, right: 16, top: 12, bottom: 0 }}>
          <CartesianGrid {...gridProps} vertical={false} />
          <XAxis dataKey="version" {...axisProps} minTickGap={16} />
          <YAxis domain={[40, 80]} ticks={[40, 50, 60, 70, 80]} unit="%" {...axisProps} />
          <ReferenceLine y={50} stroke="var(--color-muted-foreground)" strokeDasharray="4 4" label={{ value: 'coin flip', position: 'insideBottomRight', fill: 'var(--color-muted-foreground)', fontSize: 11 }} />
          {ceiling !== null && (
            <ReferenceLine
              y={Math.round(ceiling * 1000) / 10}
              stroke="var(--color-muted-foreground)"
              strokeDasharray="2 4"
              label={{ value: 'all data in one place', position: 'insideTopRight', fill: 'var(--color-muted-foreground)', fontSize: 11 }}
            />
          )}
          <Tooltip {...tooltipProps} formatter={(value) => [`${value}%`, 'Accuracy']} cursor={{ stroke: 'var(--color-muted-foreground)', strokeDasharray: '3 3' }} />
          <Line
            type="monotone"
            dataKey="accuracy"
            name="Accuracy"
            stroke="var(--color-primary)"
            strokeWidth={2}
            dot={{ r: 3, strokeWidth: 0, fill: 'var(--color-primary)' }}
            activeDot={{ r: 5, strokeWidth: 2, stroke: 'var(--color-card)' }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </ChartFrame>
  )
}
