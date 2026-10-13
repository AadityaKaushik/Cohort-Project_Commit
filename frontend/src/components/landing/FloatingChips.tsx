import { useEffect, useRef, type CSSProperties, type ReactNode } from 'react'
import { BellIcon } from 'lucide-react'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { useReducedMotion } from '@/hooks/use-motion'
import { cn } from '@/lib/utils'

interface ChipProps {
  className: string
  depth: number
  duration: number
  delay: number
  children: ReactNode
}

function Chip({ className, depth, duration, delay, children }: ChipProps) {
  const style = {
    '--depth': depth,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
  } as CSSProperties
  return (
    <div style={style} className={cn('parallax-layer animate-float surface-raised absolute rounded-xl border p-2.5', className)}>
      {children}
    </div>
  )
}

/**
 * Small sample UI pieces that drift around the hero preview. They use only transform, follow the pointer
 * slightly on wide screens, and stay put under reduced motion. Decorative, so hidden from assistive tech.
 */
export function FloatingChips({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()

  useEffect(() => {
    const element = ref.current
    if (!element || reduced || !window.matchMedia?.('(pointer: fine)').matches) {
      return
    }
    let frame = 0
    const handleMove = (event: PointerEvent) => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => {
        const x = event.clientX / window.innerWidth - 0.5
        const y = event.clientY / window.innerHeight - 0.5
        element.style.setProperty('--px', x.toFixed(3))
        element.style.setProperty('--py', y.toFixed(3))
      })
    }
    window.addEventListener('pointermove', handleMove, { passive: true })
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('pointermove', handleMove)
    }
  }, [reduced])

  return (
    <div ref={ref} className="relative">
      <div className="parallax-layer" style={{ '--depth': 6 } as CSSProperties}>
        {children}
      </div>
      <div aria-hidden className="pointer-events-none absolute inset-0 hidden lg:block">
        <Chip className="bottom-10 -left-16 flex items-center gap-2" depth={22} duration={7} delay={0}>
          <span className="text-xs font-medium">Atlas Health</span>
          <StatusBadge status="OFFER" />
        </Chip>
        <Chip className="-top-8 -left-12 flex w-60 items-start gap-2.5" depth={30} duration={9} delay={-2}>
          <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-info/12 text-info">
            <BellIcon className="size-3.5" />
          </span>
          <span className="text-left text-xs">
            <span className="block font-medium">Interview tomorrow, 10:00</span>
            <span className="text-muted-foreground">Kestrel Systems · Technical</span>
          </span>
        </Chip>
        <Chip className="top-40 -right-20 flex items-center gap-2.5" depth={16} duration={8} delay={-4}>
          <ProgressRing value={68} max={100} label="Sample mastery" size={40} stroke={5}>
            <span className="text-[10px] font-semibold tabular-nums">68</span>
          </ProgressRing>
          <span className="pr-1 text-left text-xs">
            <span className="block font-medium">Arrays</span>
            <span className="text-muted-foreground">mastery</span>
          </span>
        </Chip>
      </div>
    </div>
  )
}
