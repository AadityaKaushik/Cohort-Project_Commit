/** Series colours in fixed order: the first entity is always violet, the second always blue-teal; validated for colour-vision separation in both themes. */
export const SERIES_COLORS = ['var(--color-series-1)', 'var(--color-series-2)'] as const

export const CHART_ANIMATION_MS = 900

export const axisProps = {
  stroke: 'var(--color-border)',
  tick: { fill: 'var(--color-muted-foreground)', fontSize: 12 },
  tickLine: false,
  axisLine: false,
  tickMargin: 8,
} as const

export const gridProps = {
  stroke: 'var(--color-border)',
  strokeDasharray: '3 4',
} as const

export const tooltipProps = {
  contentStyle: {
    background: 'var(--color-popover)',
    border: '1px solid var(--color-border)',
    borderRadius: 10,
    boxShadow: 'var(--elevation)',
    fontSize: 12,
    padding: '8px 10px',
  },
  labelStyle: { color: 'var(--color-foreground)', fontWeight: 600, marginBottom: 4 },
  itemStyle: { color: 'var(--color-foreground)', padding: 0 },
} as const
