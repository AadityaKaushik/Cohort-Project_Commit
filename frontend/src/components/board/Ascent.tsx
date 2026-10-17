import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { FlapText } from '@/components/board/FlapText'
import { LINE_BG, type LineColour } from '@/lib/stage-lines'
import { cn } from '@/lib/utils'

export interface AscentStage {
  key: string
  label: string
  count: number
  line: LineColour
  to?: string
}

const BASE_STEP_REM = 1.5
const STEP_RISE_REM = 1.1
const STAGGER_MS = 90

/**
 * The pipeline drawn as a staircase: each stage is one step higher than the last, so moving an
 * application right is literally a step up. The count sits on top of each step.
 * `inverse` is for the pine brand panels, where the steps are drawn in ivory and the last one in apricot.
 */
export function Ascent({
  stages,
  inverse = false,
  showCounts = true,
  className,
}: {
  stages: AscentStage[]
  inverse?: boolean
  showCounts?: boolean
  className?: string
}) {
  const last = stages.length - 1

  function stepColour(stage: AscentStage, index: number) {
    if (!inverse) return LINE_BG[stage.line]
    return index === last ? 'bg-signal' : 'bg-brand-foreground'
  }

  function stepOpacity(index: number) {
    if (!inverse || index === last) return undefined
    return 0.28 + (0.6 * index) / Math.max(1, last - 1)
  }

  return (
    <div className={className}>
      <ol aria-label="Pipeline" className="hidden items-end sm:grid" style={{ gridTemplateColumns: `repeat(${stages.length}, minmax(0, 1fr))` }}>
        {stages.map((stage, index) => (
          <li key={stage.key} className="min-w-0">
            <StageLink stage={stage} className="group flex flex-col">
              {showCounts && (
                <FlapText value={stage.count} className="text-display px-1 pb-1.5 text-[2.4rem] leading-none tabular-nums transition-transform duration-200 group-hover:-translate-y-1" />
              )}
              <span
                aria-hidden
                data-step
                className={cn(
                  'block origin-bottom border-r-2 border-card transition-transform duration-200 group-hover:-translate-y-1 motion-safe:animate-[step-rise_560ms_cubic-bezier(0.22,1,0.36,1)_both]',
                  inverse && 'border-brand',
                  stepColour(stage, index),
                )}
                style={{
                  height: `${BASE_STEP_REM + index * STEP_RISE_REM}rem`,
                  opacity: stepOpacity(index),
                  animationDelay: `${index * STAGGER_MS}ms`,
                }}
              />
              <span className={cn('board-label truncate border-t-2 px-1 pt-2', inverse ? 'border-brand-foreground/40' : 'border-rule')}>{stage.label}</span>
            </StageLink>
          </li>
        ))}
      </ol>

      <ol aria-label="Pipeline" className="sm:hidden">
        {stages.map((stage, index) => (
          <li key={stage.key}>
            <StageLink stage={stage} className="flex min-h-11 items-center gap-3 border-b border-current/15 py-1.5">
              <span className="w-24 shrink-0">
                <span
                  aria-hidden
                  className={cn('block h-5 origin-left motion-safe:animate-[step-rise_480ms_cubic-bezier(0.22,1,0.36,1)_both]', stepColour(stage, index))}
                  style={{ width: `${(100 * (index + 1)) / stages.length}%`, opacity: stepOpacity(index), animationDelay: `${index * STAGGER_MS}ms` }}
                />
              </span>
              <span className="board-label flex-1 truncate">{stage.label}</span>
              {showCounts && <FlapText value={stage.count} className="text-display text-2xl" />}
            </StageLink>
          </li>
        ))}
      </ol>
    </div>
  )
}

function StageLink({ stage, className, children }: { stage: AscentStage; className?: string; children: ReactNode }) {
  if (!stage.to) {
    return <div className={className}>{children}</div>
  }
  return (
    <Link to={stage.to} className={cn('rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4', className)} aria-label={`${stage.label}: ${stage.count}`}>
      {children}
    </Link>
  )
}
