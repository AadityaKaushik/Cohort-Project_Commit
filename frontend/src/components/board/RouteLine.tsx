import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { FlapText } from '@/components/board/FlapText'
import { LINE_BG, LINE_BORDER, type LineColour } from '@/lib/stage-lines'
import { cn } from '@/lib/utils'

export interface RouteStation {
  key: string
  label: string
  count: number
  line: LineColour
  to?: string
}

/**
 * The pipeline drawn as one transit line: each stage is a station with its count.
 * The segment leaving a station carries that station's line colour, and the line draws in once on load.
 */
export function RouteLine({ stations, className }: { stations: RouteStation[]; className?: string }) {
  return (
    <div className={className}>
      <ol aria-label="Pipeline" className="hidden sm:grid" style={{ gridTemplateColumns: `repeat(${stations.length}, minmax(0, 1fr))` }}>
        {stations.map((station, index) => (
          <li key={station.key} className="min-w-0">
            <StationLink station={station} className="group flex flex-col items-center gap-2 text-center">
              <FlapText value={station.count} className="text-display text-[2.6rem] leading-none tabular-nums group-hover:underline group-hover:decoration-signal group-hover:decoration-4 group-hover:underline-offset-4" />
              <span className="relative block h-6 w-full">
                {index < stations.length - 1 && (
                  <span
                    aria-hidden
                    className={cn('absolute top-1/2 left-1/2 h-[6px] w-full origin-left -translate-y-1/2 motion-safe:animate-[route-grow_520ms_cubic-bezier(0.65,0,0.35,1)_both]', LINE_BG[station.line])}
                    style={{ animationDelay: `${index * 110}ms` }}
                  />
                )}
                <span
                  aria-hidden
                  className={cn(
                    'absolute top-1/2 left-1/2 z-10 size-5 -translate-x-1/2 -translate-y-1/2 rounded-full border-[5px] bg-background transition-transform group-hover:scale-125 motion-safe:animate-[station-in_260ms_ease-out_both]',
                    LINE_BORDER[station.line],
                  )}
                  style={{ animationDelay: `${index * 110}ms` }}
                />
              </span>
              <span className="board-label truncate">{station.label}</span>
            </StationLink>
          </li>
        ))}
      </ol>

      <ol aria-label="Pipeline" className="sm:hidden">
        {stations.map((station, index) => (
          <li key={station.key}>
            <StationLink station={station} className="flex items-stretch gap-3">
              <span className="relative w-5 shrink-0">
                {index < stations.length - 1 && (
                  <span aria-hidden className={cn('absolute top-1/2 left-1/2 h-full w-[5px] -translate-x-1/2', LINE_BG[station.line])} />
                )}
                <span
                  aria-hidden
                  className={cn('absolute top-1/2 left-1/2 z-10 size-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 bg-background', LINE_BORDER[station.line])}
                />
              </span>
              <span className="flex min-h-11 flex-1 items-center justify-between border-b border-border py-2">
                <span className="board-label">{station.label}</span>
                <FlapText value={station.count} className="text-display text-2xl" />
              </span>
            </StationLink>
          </li>
        ))}
      </ol>
    </div>
  )
}

function StationLink({ station, className, children }: { station: RouteStation; className?: string; children: ReactNode }) {
  if (!station.to) {
    return <div className={className}>{children}</div>
  }
  return (
    <Link to={station.to} className={cn('rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4', className)} aria-label={`${station.label}: ${station.count}`}>
      {children}
    </Link>
  )
}
