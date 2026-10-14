import { flushSync } from 'react-dom'
import { prefersReducedMotion } from '@/hooks/use-motion'

const ROUTE_TRANSITION_ATTRIBUTE = 'routeTransition'

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

export function finishSignIn(navigate: () => void): void {
  if (prefersReducedMotion()) {
    navigate()
    return
  }
  window.setTimeout(() => runRouteTransition(navigate), SUCCESS_HOLD_MS)
}
