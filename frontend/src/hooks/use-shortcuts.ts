import { useEffect, useRef } from 'react'

const CHORD_WINDOW_MS = 1000

export type ShortcutAction = 'help' | 'new-application' | 'search' | 'go-applications' | 'go-dashboard' | 'go-prep'

export const SHORTCUTS: { keys: string[]; label: string }[] = [
  { keys: ['?'], label: 'Show this list' },
  { keys: ['n'], label: 'New application' },
  { keys: ['/'], label: 'Search' },
  { keys: ['g', 'a'], label: 'Go to applications' },
  { keys: ['g', 'd'], label: 'Go to dashboard' },
  { keys: ['g', 'p'], label: 'Go to prep' },
]

const GO_TARGETS: Record<string, ShortcutAction> = {
  a: 'go-applications',
  d: 'go-dashboard',
  p: 'go-prep',
}

export function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false
  }
  return target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)
}

export function useShortcuts(onAction: (action: ShortcutAction) => void) {
  const handler = useRef(onAction)
  useEffect(() => {
    handler.current = onAction
  })

  useEffect(() => {
    let chordStartedAt = 0

    function handleKeyDown(event: KeyboardEvent) {
      if (event.metaKey || event.ctrlKey || event.altKey || isTypingTarget(event.target)) {
        return
      }
      if (document.querySelector('[role="dialog"]')) {
        return
      }
      const inChord = Date.now() - chordStartedAt < CHORD_WINDOW_MS
      chordStartedAt = 0

      if (inChord && GO_TARGETS[event.key]) {
        event.preventDefault()
        handler.current(GO_TARGETS[event.key])
      } else if (event.key === 'g') {
        chordStartedAt = Date.now()
      } else if (event.key === '?') {
        event.preventDefault()
        handler.current('help')
      } else if (event.key === 'n') {
        event.preventDefault()
        handler.current('new-application')
      } else if (event.key === '/') {
        event.preventDefault()
        handler.current('search')
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])
}
