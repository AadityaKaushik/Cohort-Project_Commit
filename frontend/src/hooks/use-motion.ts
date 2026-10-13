import { useCallback, useEffect, useRef, useState, type RefObject } from 'react'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

export function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(REDUCED_MOTION_QUERY).matches
}

export function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(prefersReducedMotion)

  useEffect(() => {
    if (typeof window.matchMedia !== 'function') {
      return
    }
    const query = window.matchMedia(REDUCED_MOTION_QUERY)
    const handleChange = (event: MediaQueryListEvent) => setReduced(event.matches)
    query.addEventListener('change', handleChange)
    return () => query.removeEventListener('change', handleChange)
  }, [])

  return reduced
}

/** True once the element has scrolled into view; immediately true where IntersectionObserver is unavailable. */
export function useInView<T extends Element>(): [RefObject<T | null>, boolean] {
  const ref = useRef<T>(null)
  const [inView, setInView] = useState(() => typeof IntersectionObserver === 'undefined')

  useEffect(() => {
    const element = ref.current
    if (inView || !element || typeof IntersectionObserver === 'undefined') {
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setInView(true)
          observer.disconnect()
        }
      },
      { rootMargin: '0px 0px -10% 0px', threshold: 0.15 },
    )
    observer.observe(element)
    return () => observer.disconnect()
  }, [inView])

  return [ref, inView]
}

const COUNT_UP_MS = 900

function formatCount(value: number, decimals: number, suffix: string): string {
  return `${value.toFixed(decimals)}${suffix}`
}

/**
 * Eases a number from zero to the target on first render by writing straight to the element each frame,
 * so React never re-renders mid-animation. Under reduced motion the target is rendered straight away.
 */
export function useCountUp<T extends HTMLElement>(target: number, decimals = 0, suffix = ''): { ref: RefObject<T | null>; text: string } {
  const ref = useRef<T>(null)
  const reduced = useReducedMotion()
  const animatable = !reduced && typeof window.requestAnimationFrame === 'function'

  useEffect(() => {
    const element = ref.current
    if (!animatable || !element) {
      return
    }
    let frame = 0
    const start = performance.now()
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / COUNT_UP_MS)
      element.textContent = formatCount(target * (1 - (1 - progress) ** 3), decimals, suffix)
      if (progress < 1) {
        frame = window.requestAnimationFrame(tick)
      }
    }
    frame = window.requestAnimationFrame(tick)
    return () => window.cancelAnimationFrame(frame)
  }, [target, decimals, suffix, animatable])

  return { ref, text: formatCount(animatable ? 0 : target, decimals, suffix) }
}

/** Coalesces a high-frequency handler (pointer moves, scrolls) into at most one call per animation frame, using the latest event. */
export function useFrameThrottle<E>(handler: (event: E) => void): (event: E) => void {
  const handlerRef = useRef(handler)
  const latest = useRef<E | null>(null)
  const frame = useRef(0)

  useEffect(() => {
    handlerRef.current = handler
  })

  useEffect(() => () => window.cancelAnimationFrame(frame.current), [])

  return useCallback((event: E) => {
    latest.current = event
    if (frame.current) {
      return
    }
    frame.current = window.requestAnimationFrame(() => {
      frame.current = 0
      if (latest.current !== null) {
        handlerRef.current(latest.current)
      }
    })
  }, [])
}
