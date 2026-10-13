import { useEffect, useRef } from 'react'
import { useReducedMotion } from '@/hooks/use-motion'

const SPOTLIGHT_SIZE = 560

/** Soft aurora light, a faint grid, and a spotlight that follows the pointer. Transform and opacity only. */
export function HeroBackdrop() {
  const rootRef = useRef<HTMLDivElement>(null)
  const spotRef = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()

  useEffect(() => {
    const root = rootRef.current
    const spot = spotRef.current
    if (!root || !spot || reduced || !window.matchMedia?.('(pointer: fine)').matches) {
      return
    }
    let frame = 0
    const handleMove = (event: PointerEvent) => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => {
        const bounds = root.getBoundingClientRect()
        const x = event.clientX - bounds.left - SPOTLIGHT_SIZE / 2
        const y = event.clientY - bounds.top - SPOTLIGHT_SIZE / 2
        spot.style.transform = `translate3d(${x}px, ${y}px, 0)`
        spot.style.opacity = event.clientY < bounds.bottom ? '1' : '0'
      })
    }
    window.addEventListener('pointermove', handleMove, { passive: true })
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('pointermove', handleMove)
    }
  }, [reduced])

  return (
    <div ref={rootRef} aria-hidden className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
      <div className="aurora absolute -top-40 left-1/2 h-[520px] w-[900px] -translate-x-1/2 rounded-full bg-primary/20 blur-[120px] dark:bg-primary/25" />
      <div
        className="aurora absolute -top-24 left-[58%] h-[360px] w-[520px] rounded-full bg-chart-2/15 blur-[110px]"
        style={{ animationDuration: '24s', animationDelay: '-6s' }}
      />
      <div className="hero-grid absolute inset-0" />
      <div
        ref={spotRef}
        className="absolute top-0 left-0 rounded-full opacity-0 transition-opacity duration-500"
        style={{
          width: SPOTLIGHT_SIZE,
          height: SPOTLIGHT_SIZE,
          background: 'radial-gradient(closest-side, var(--glow), transparent)',
        }}
      />
    </div>
  )
}
