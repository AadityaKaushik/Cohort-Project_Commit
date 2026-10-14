import { useCallback, useSyncExternalStore } from 'react'
import { useToast } from '@/hooks/use-toast'

export const UNDO_WINDOW_MS = 6000

const pending = new Set<string>()
const listeners = new Set<() => void>()
let snapshot: ReadonlySet<string> = new Set()

function publish() {
  snapshot = new Set(pending)
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function usePendingDeletes(): ReadonlySet<string> {
  return useSyncExternalStore(subscribe, () => snapshot)
}

interface UndoableDelete {
  id: string
  title: string
  send: () => Promise<unknown>
  onSettled: () => void
  errorTitle: string
}

// The timer is module-level so the delete still goes out after the user leaves the page that started it.
export function useUndoableDelete() {
  const { toast } = useToast()

  return useCallback(
    ({ id, title, send, onSettled, errorTitle }: UndoableDelete) => {
      pending.add(id)
      publish()
      let undone = false
      const timer = window.setTimeout(() => {
        if (undone) {
          return
        }
        send()
          .catch(() => toast({ title: errorTitle, variant: 'error' }))
          .finally(() => {
            pending.delete(id)
            publish()
            onSettled()
          })
      }, UNDO_WINDOW_MS)
      toast({
        title,
        durationMs: UNDO_WINDOW_MS,
        action: {
          label: 'Undo',
          onClick: () => {
            undone = true
            window.clearTimeout(timer)
            pending.delete(id)
            publish()
          },
        },
      })
    },
    [toast],
  )
}
