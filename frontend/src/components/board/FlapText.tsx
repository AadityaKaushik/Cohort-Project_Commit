import { useEffect, useState } from 'react'
import { prefersReducedMotion } from '@/lib/motion'
import { cn } from '@/lib/utils'

const COUNT_MS = 650

/**
 * Numbers count up to their value and text rises into place, once, when it first appears.
 * Screen readers get the final value straight away; reduced-motion users see no animation.
 */
export function FlapText({ value, className }: { value: string | number; className?: string }) {
  const text = String(value)
  const [shown, setShown] = useState(text)
  const isCount = typeof value === 'number' && Number.isInteger(value) && value > 0

  useEffect(() => {
    if (!isCount || prefersReducedMotion()) {
      setShown(text)
      return
    }
    const target = value as number
    const start = performance.now()
    let frame = 0
    const step = (now: number) => {
      const progress = Math.min(1, (now - start) / COUNT_MS)
      const eased = 1 - (1 - progress) ** 3
      setShown(String(Math.round(target * eased)))
      if (progress < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [isCount, text, value])

  return (
    <span className={cn('tabular inline-block', !isCount && 'motion-safe:animate-[rise-in_420ms_ease-out_both]', className)} aria-label={text} role="text">
      <span aria-hidden>{shown}</span>
    </span>
  )
}
