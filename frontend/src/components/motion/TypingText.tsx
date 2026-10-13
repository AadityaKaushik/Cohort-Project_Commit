import { useEffect, useState } from 'react'
import { useReducedMotion } from '@/hooks/use-motion'
import { cn } from '@/lib/utils'

const TYPE_MS = 55
const ERASE_MS = 28
const HOLD_MS = 1800
const GAP_MS = 350

interface TypingTextProps {
  phrases: readonly string[]
  className?: string
  caretClassName?: string
  loop?: boolean
}

/**
 * Types and erases each phrase in turn. The longest phrase is laid out invisibly underneath so the line
 * never changes width, screen readers get every phrase as plain text, and it pauses while the tab is hidden.
 * Under reduced motion it shows the first phrase, fully written, and never moves.
 */
export function TypingText({ phrases, className, caretClassName, loop = true }: TypingTextProps) {
  const reduced = useReducedMotion()
  const [phraseIndex, setPhraseIndex] = useState(0)
  const [length, setLength] = useState(0)
  const [erasing, setErasing] = useState(false)
  const [hidden, setHidden] = useState(() => typeof document !== 'undefined' && document.hidden)

  useEffect(() => {
    const handleVisibility = () => setHidden(document.hidden)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => document.removeEventListener('visibilitychange', handleVisibility)
  }, [])

  const phrase = phrases[phraseIndex]
  const finished = !loop && phraseIndex === phrases.length - 1 && length === phrase.length

  useEffect(() => {
    if (reduced || hidden || finished) {
      return
    }
    let delay = erasing ? ERASE_MS : TYPE_MS
    let step = () => setLength((current) => current + (erasing ? -1 : 1))
    if (!erasing && length === phrase.length) {
      delay = HOLD_MS
      step = () => setErasing(true)
    } else if (erasing && length === 0) {
      delay = GAP_MS
      step = () => {
        setErasing(false)
        setPhraseIndex((current) => (current + 1) % phrases.length)
      }
    }
    const timer = window.setTimeout(step, delay)
    return () => window.clearTimeout(timer)
  }, [reduced, hidden, finished, erasing, length, phrase.length, phrases.length])

  const longest = phrases.reduce((a, b) => (b.length > a.length ? b : a), '')
  const shown = reduced ? phrases[0] : phrase.slice(0, length)

  return (
    <span className={cn('inline-grid text-left align-baseline', className)}>
      <span className="sr-only">{phrases.join(' ')}</span>
      <span aria-hidden className="invisible col-start-1 row-start-1 whitespace-nowrap">
        {longest}
        <span className="inline-block w-[0.08em]" />
      </span>
      <span aria-hidden data-testid="typing-text" className="col-start-1 row-start-1 whitespace-nowrap">
        {shown}
        <span
          className={cn(
            'ml-[0.04em] inline-block h-[0.95em] w-[0.08em] translate-y-[0.12em] rounded-full bg-current',
            !reduced && 'animate-caret',
            caretClassName,
          )}
        />
      </span>
    </span>
  )
}
