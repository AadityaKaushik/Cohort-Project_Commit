import { useRef, type PointerEvent, type ReactNode } from 'react'
import { useFrameThrottle, useReducedMotion } from '@/hooks/use-motion'

const PULL = 0.18

/** Leans its child a few pixels towards a mouse pointer; still for touch and reduced motion. */
export function Magnetic({ children, className }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()

  const pull = useFrameThrottle(({ clientX, clientY }: { clientX: number; clientY: number }) => {
    const element = ref.current
    if (!element) return
    const bounds = element.getBoundingClientRect()
    const x = (clientX - (bounds.left + bounds.width / 2)) * PULL
    const y = (clientY - (bounds.top + bounds.height / 2)) * PULL
    element.style.transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0)`
  })

  function handleMove(event: PointerEvent<HTMLDivElement>) {
    if (reduced || event.pointerType !== 'mouse') return
    pull({ clientX: event.clientX, clientY: event.clientY })
  }

  function handleLeave() {
    if (ref.current) ref.current.style.transform = ''
  }

  return (
    <div
      ref={ref}
      onPointerMove={handleMove}
      onPointerLeave={handleLeave}
      className={['transition-transform duration-200 ease-out', className].filter(Boolean).join(' ')}
    >
      {children}
    </div>
  )
}
