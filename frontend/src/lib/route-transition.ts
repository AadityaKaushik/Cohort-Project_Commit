import { flushSync } from 'react-dom'
import { prefersReducedMotion } from '@/hooks/use-motion'

const ROUTE_TRANSITION_ATTRIBUTE = 'routeTransition'

/**
 * Swaps routes inside a view transition so the outgoing page fades out, the
 * incoming one slides in and named elements such as the brand mark morph
 * between them. Without the API, or under reduced motion, the swap is instant.
 */
export function runRouteTransition(update: () => void): void {
  if (prefersReducedMotion() || typeof document.startViewTransition !== 'function') {
    update()
    return
  }
  const root = document.documentElement
  root.dataset[ROUTE_TRANSITION_ATTRIBUTE] = 'true'
  const transition = document.startViewTransition(() => flushSync(update))
  transition.finished
    .catch(() => undefined)
    .finally(() => {
      delete root.dataset[ROUTE_TRANSITION_ATTRIBUTE]
    })
}

const SUCCESS_HOLD_MS = 450

/** Lets a sign-in's success state register for a beat, then moves on inside a route transition; instant under reduced motion. */
export function finishSignIn(navigate: () => void): void {
  if (prefersReducedMotion()) {
    navigate()
    return
  }
  window.setTimeout(() => runRouteTransition(navigate), SUCCESS_HOLD_MS)
}
