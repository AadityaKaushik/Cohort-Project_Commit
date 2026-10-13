import { useRef, type PointerEvent, type ReactNode } from 'react'
import { useReducedMotion } from '@/hooks/use-motion'

const PULL = 0.18

/** Leans its child a few pixels towards a mouse pointer; still for touch and reduced motion. */
export function Magnetic({ children, className }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()

  function handleMove(event: PointerEvent<HTMLDivElement>) {
    const element = ref.current
    if (!element || reduced || event.pointerType !== 'mouse') return
    const bounds = element.getBoundingClientRect()
    const x = (event.clientX - (bounds.left + bounds.width / 2)) * PULL
    const y = (event.clientY - (bounds.top + bounds.height / 2)) * PULL
    element.style.transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0)`
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
