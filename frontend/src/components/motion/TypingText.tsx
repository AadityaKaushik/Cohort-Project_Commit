import { useEffect, useRef, useState } from 'react'
import { useInView, useReducedMotion } from '@/hooks/use-motion'
import { cn } from '@/lib/utils'

const TYPE_MS = 28
const ERASE_MS = 16
const HOLD_MS = 2200
const GAP_MS = 300
const SESSION_STORE = 'cohort.typed'

function typedKeys(): string[] {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_STORE) ?? '[]') as string[]
  } catch {
    return []
  }
}

function alreadyTyped(key: string | undefined): boolean {
  return key !== undefined && typedKeys().includes(key)
}

function markTyped(key: string | undefined) {
  if (key === undefined || alreadyTyped(key)) {
    return
  }
  try {
    sessionStorage.setItem(SESSION_STORE, JSON.stringify([...typedKeys(), key]))
  } catch {
    return
  }
}

function usePageVisible(): boolean {
  const [visible, setVisible] = useState(() => typeof document === 'undefined' || !document.hidden)
  useEffect(() => {
    const handleVisibility = () => setVisible(!document.hidden)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => document.removeEventListener('visibilitychange', handleVisibility)
  }, [])
  return visible
}

function Caret({ blinking, className }: { blinking: boolean; className?: string }) {
  return (
    <span
      className={cn(
        'ml-[0.04em] inline-block h-[0.95em] w-[0.08em] translate-y-[0.12em] rounded-full bg-current opacity-80',
        blinking && 'animate-caret',
        className,
      )}
    />
  )
}

interface TypingTextProps {
  /** One string types once; several strings type, hold and erase in turn. */
  text: string | readonly string[]
  /** Types this string only once per browser session; later visits show it already written. */
  sessionKey?: string
  /** Loops through several strings; ignored for a single string. */
  loop?: boolean
  startDelay?: number
  className?: string
  caretClassName?: string
}

/**
 * The one typing effect. The full text always occupies its final space (so nothing shifts) and is what
 * screen readers and text search see; the moving letters are drawn as generated content on top. It starts
 * when scrolled into view, pauses while the tab is hidden, and under reduced motion simply shows the text.
 */
export function TypingText({ text, sessionKey, loop = true, startDelay = 0, className, caretClassName }: TypingTextProps) {
  if (typeof text !== 'string' && text.length > 1) {
    return <CyclingText phrases={text} loop={loop} className={className} caretClassName={caretClassName} />
  }
  const single = typeof text === 'string' ? text : text[0]
  return <TypeOnce key={single} text={single} sessionKey={sessionKey} startDelay={startDelay} className={className} caretClassName={caretClassName} />
}

function TypeOnce({ text, sessionKey, startDelay, className, caretClassName }: { text: string; sessionKey?: string; startDelay: number; className?: string; caretClassName?: string }) {
  const reduced = useReducedMotion()
  const visible = usePageVisible()
  const [ref, inView] = useInView<HTMLSpanElement>()
  const overlayRef = useRef<HTMLSpanElement>(null)
  const typedRef = useRef(0)
  const [skip] = useState(() => alreadyTyped(sessionKey))
  const [finished, setFinished] = useState(false)
  const done = reduced || skip || finished

  useEffect(() => {
    if (done || !inView || !visible) {
      return
    }
    let timer = 0
    const step = () => {
      typedRef.current += 1
      overlayRef.current?.setAttribute('data-typed', text.slice(0, typedRef.current))
      if (typedRef.current >= text.length) {
        markTyped(sessionKey)
        setFinished(true)
        return
      }
      timer = window.setTimeout(step, TYPE_MS)
    }
    timer = window.setTimeout(step, typedRef.current === 0 ? startDelay + TYPE_MS : TYPE_MS)
    return () => window.clearTimeout(timer)
  }, [done, inView, visible, text, startDelay, sessionKey])

  return (
    <span ref={ref} data-typing={!done || undefined} className={cn('relative inline-block', className)}>
      <span className={cn(!done && 'text-transparent')}>{text}</span>
      {!done && (
        <span
          ref={overlayRef}
          aria-hidden
          data-testid="typing-text"
          data-typed=""
          className="pointer-events-none absolute inset-0 select-none before:content-[attr(data-typed)]"
        >
          <Caret blinking={false} className={caretClassName} />
        </span>
      )}
    </span>
  )
}

function CyclingText({ phrases, loop, className, caretClassName }: { phrases: readonly string[]; loop: boolean; className?: string; caretClassName?: string }) {
  const reduced = useReducedMotion()
  const visible = usePageVisible()
  const overlayRef = useRef<HTMLSpanElement>(null)
  const cursor = useRef({ phrase: 0, length: 0, erasing: false })

  useEffect(() => {
    if (reduced || !visible) {
      return
    }
    let timer = 0
    const step = () => {
      const state = cursor.current
      const phrase = phrases[state.phrase]
      let delay = state.erasing ? ERASE_MS : TYPE_MS
      if (!state.erasing && state.length === phrase.length) {
        if (!loop && state.phrase === phrases.length - 1) {
          return
        }
        state.erasing = true
        delay = HOLD_MS
      } else if (state.erasing && state.length === 0) {
        state.erasing = false
        state.phrase = (state.phrase + 1) % phrases.length
        delay = GAP_MS
      } else {
        state.length += state.erasing ? -1 : 1
        overlayRef.current?.setAttribute('data-typed', phrases[state.phrase].slice(0, state.length))
      }
      timer = window.setTimeout(step, delay)
    }
    timer = window.setTimeout(step, TYPE_MS)
    return () => window.clearTimeout(timer)
  }, [reduced, visible, loop, phrases])

  const longest = phrases.reduce((a, b) => (b.length > a.length ? b : a), '')

  return (
    <span className={cn('inline-grid text-left align-baseline', className)}>
      <span className="sr-only">{phrases.join(' ')}</span>
      <span aria-hidden className="invisible col-start-1 row-start-1 whitespace-nowrap">
        {longest}
        <span className="inline-block w-[0.08em]" />
      </span>
      <span
        ref={overlayRef}
        aria-hidden
        data-testid="typing-text"
        data-typed={reduced ? phrases[0] : ''}
        className="col-start-1 row-start-1 whitespace-nowrap before:content-[attr(data-typed)]"
      >
        <Caret blinking={!reduced} className={caretClassName} />
      </span>
    </span>
  )
}
