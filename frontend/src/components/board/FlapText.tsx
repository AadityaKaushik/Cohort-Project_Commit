import { useEffect, useState } from 'react'
import { prefersReducedMotion } from '@/lib/motion'
import { cn } from '@/lib/utils'

const GLYPHS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'
const TICK_MS = 40

function scramble(value: string, settled: number): string {
  return value
    .split('')
    .map((char, index) => (index < settled || char === ' ' || char === ':' ? char : GLYPHS[Math.floor(Math.random() * GLYPHS.length)]))
    .join('')
}

/**
 * Text that settles one character at a time, like a split-flap departure board.
 * Screen readers get the final value straight away; reduced-motion users see no animation.
 */
export function FlapText({ value, className }: { value: string | number; className?: string }) {
  const text = String(value)
  const [shown, setShown] = useState(text)

  useEffect(() => {
    if (prefersReducedMotion()) {
      setShown(text)
      return
    }
    let tick = 0
    const settleEvery = 3
    const total = text.length * settleEvery + 4
    setShown(scramble(text, 0))
    const timer = window.setInterval(() => {
      tick += 1
      if (tick >= total) {
        window.clearInterval(timer)
        setShown(text)
        return
      }
      setShown(scramble(text, Math.floor((tick - 4) / settleEvery)))
    }, TICK_MS)
    return () => window.clearInterval(timer)
  }, [text])

  return (
    <span className={cn('tabular', className)} aria-label={text} role="text">
      <span aria-hidden>{shown}</span>
    </span>
  )
}
