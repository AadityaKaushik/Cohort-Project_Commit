import { useInView } from '@/lib/use-in-view'
import { cn } from '@/lib/utils'

const CLIENTS = ['You', 'Priya', 'Arjun', 'Mei']

/**
 * Federated learning as a picture: each person's data stays in their box, and only a clipped,
 * noised summary travels along their track to the shared model, which waits for at least three people.
 */
export function PrivacyFlow() {
  const { ref, inView } = useInView<HTMLDivElement>(0.3)
  return (
    <div ref={ref} aria-hidden className="grid grid-cols-[minmax(0,1fr)_2.5rem_minmax(0,0.85fr)] gap-x-2 gap-y-2.5 sm:grid-cols-[minmax(0,1fr)_5rem_minmax(0,0.85fr)] sm:gap-x-3">
      {CLIENTS.map((name, index) => (
        <div key={name} className="contents">
          <div className="flex items-center justify-between gap-2 rounded-sm border border-brand-foreground/25 px-3 py-2" style={{ gridRow: index + 1, gridColumn: 1 }}>
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold">{name}</span>
              <span className="block truncate text-[11px] opacity-70">applications, notes, practice</span>
            </span>
            <span
              className={cn('board-label hidden shrink-0 text-[10px] transition-opacity duration-500 sm:inline', inView ? 'opacity-100' : 'opacity-0')}
              style={{ transitionDelay: `${300 + index * 150}ms` }}
            >
              stays here
            </span>
          </div>
          <div className="relative self-center" style={{ gridRow: index + 1, gridColumn: 2 }}>
            <span className="block border-t-2 border-dashed border-brand-foreground/30" />
            <span
              className={cn('absolute -top-[5px] left-0 size-2 rounded-full bg-signal', inView && 'motion-safe:animate-[fl-travel_2.4s_ease-in-out_infinite]')}
              style={{ animationDelay: `${index * 0.45}s` }}
            />
          </div>
        </div>
      ))}
      <div
        className="flex flex-col items-center justify-center rounded-md border-2 border-signal p-3 text-center sm:p-4"
        style={{ gridRow: `1 / span ${CLIENTS.length}`, gridColumn: 3 }}
      >
        <p className="board-label text-[10px] opacity-80 sm:text-[11px]">Shared model</p>
        <p className="text-display mt-2 text-4xl text-signal">avg</p>
        <p className="mt-2 text-[11px] opacity-80 sm:text-xs">Waits for at least 3 people, then averages what arrived</p>
      </div>
      <p className="board-label text-center text-[9px] opacity-80 sm:text-[10px]" style={{ gridRow: CLIENTS.length + 1, gridColumn: 2 }}>
        clipped + noise
      </p>
    </div>
  )
}
