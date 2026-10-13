import { useEffect, useState } from 'react'
import { useReducedMotion } from '@/hooks/use-motion'
import { quoteAt, quoteOfTheDay } from '@/lib/quotes'
import { cn } from '@/lib/utils'

const ROTATE_EVERY_MS = 9000

interface RotatingQuoteProps {
  className?: string
  rotate?: boolean
  offset?: number
}

/** An understated line of encouragement. It changes with a soft fade, and holds still under reduced motion. */
export function RotatingQuote({ className, rotate = true, offset = 0 }: RotatingQuoteProps) {
  const reduced = useReducedMotion()
  const [index, setIndex] = useState(() => quoteOfTheDay() + offset)

  useEffect(() => {
    if (!rotate || reduced) {
      return
    }
    const timer = window.setInterval(() => setIndex((current) => current + 1), ROTATE_EVERY_MS)
    return () => window.clearInterval(timer)
  }, [rotate, reduced])

  return (
    <p data-testid="rotating-quote" className={cn('border-l-2 border-primary/50 pl-3 font-heading text-pretty text-muted-foreground', className)}>
      <span key={index} className="inline-block animate-quote">
        {quoteAt(index)}
      </span>
    </p>
  )
}
