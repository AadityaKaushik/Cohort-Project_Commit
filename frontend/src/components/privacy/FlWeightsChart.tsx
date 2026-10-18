import { useState } from 'react'
import { featureLabel, type FlWeight } from '@/lib/fl-demo-api'
import { cn } from '@/lib/utils'

/**
 * The shared model's 24 weights as bars on either side of zero, largest first. Right of zero pushes the
 * prediction towards "will feel confident", left pushes it away; the side carries the meaning, colour backs it up.
 */
export function FlWeightsChart({ weights }: { weights: FlWeight[] }) {
  const [active, setActive] = useState<string | null>(null)
  const sorted = [...weights].sort((a, b) => Math.abs(b.value) - Math.abs(a.value))
  const scale = Math.max(0.01, ...weights.map((weight) => Math.abs(weight.value)))
  const allZero = weights.every((weight) => weight.value === 0)

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-[minmax(0,10rem)_minmax(0,1fr)_minmax(0,1fr)] items-end gap-x-2 text-xs text-muted-foreground sm:grid-cols-[minmax(0,13rem)_minmax(0,1fr)_minmax(0,1fr)]">
        <span />
        <span className="text-right">less likely to feel confident</span>
        <span>more likely to feel confident</span>
      </div>
      <ul aria-label="Model weights" className="space-y-[2px]">
        {sorted.map((weight) => {
          const width = `${(Math.abs(weight.value) / scale) * 100}%`
          const positive = weight.value > 0
          return (
            <li
              key={weight.feature}
              tabIndex={0}
              onMouseEnter={() => setActive(weight.feature)}
              onMouseLeave={() => setActive(null)}
              onFocus={() => setActive(weight.feature)}
              onBlur={() => setActive(null)}
              aria-label={`${featureLabel(weight.feature)}: ${weight.value.toFixed(3)}`}
              className={cn(
                'relative grid grid-cols-[minmax(0,10rem)_minmax(0,1fr)_minmax(0,1fr)] items-center gap-x-2 rounded-sm py-0.5 outline-none focus-visible:ring-2 focus-visible:ring-ring sm:grid-cols-[minmax(0,13rem)_minmax(0,1fr)_minmax(0,1fr)]',
                active === weight.feature && 'bg-muted',
              )}
            >
              <span className="truncate text-xs">{featureLabel(weight.feature)}</span>
              <span className="flex h-3.5 justify-end border-r border-foreground/40">
                {!positive && weight.value !== 0 && <span className="h-full rounded-l-[4px] bg-chart-down" style={{ width }} />}
              </span>
              <span className="flex h-3.5">
                {positive && <span className="h-full rounded-r-[4px] bg-chart-up" style={{ width }} />}
              </span>
              {active === weight.feature && (
                <span role="tooltip" className="tabular absolute top-full left-40 z-10 mt-1 rounded-sm border bg-popover px-2 py-1 text-xs text-popover-foreground sm:left-52">
                  {featureLabel(weight.feature)}: {weight.value >= 0 ? '+' : ''}
                  {weight.value.toFixed(3)}
                </span>
              )}
            </li>
          )
        })}
      </ul>
      {allZero && <p className="text-sm text-muted-foreground">Every weight is 0: version 0 has not learned anything yet. Run a round.</p>}
    </div>
  )
}
