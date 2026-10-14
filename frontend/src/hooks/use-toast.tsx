import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { CheckCircle2, CircleAlert, X } from 'lucide-react'
import { cn } from '@/lib/utils'

export type ToastVariant = 'success' | 'error' | 'info'

export interface ToastInput {
  title: string
  description?: string
  variant?: ToastVariant
}

interface ToastItem extends ToastInput {
  id: number
  leaving?: boolean
}

interface ToastContextValue {
  toast: (input: ToastInput) => void
}

const DISMISS_AFTER_MS = 5000
const EXIT_MS = 180

const ToastContext = createContext<ToastContextValue>({ toast: () => undefined })

let nextId = 1

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => {
    setItems((current) => current.map((item) => (item.id === id ? { ...item, leaving: true } : item)))
    window.setTimeout(() => setItems((current) => current.filter((item) => item.id !== id)), EXIT_MS)
  }, [])

  const toast = useCallback(
    (input: ToastInput) => {
      const id = nextId++
      setItems((current) => [...current.slice(-3), { ...input, id }])
      window.setTimeout(() => dismiss(id), DISMISS_AFTER_MS)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-4 bottom-4 z-[100] flex flex-col items-end gap-2 sm:left-auto sm:w-96"
      >
        {items.map((item) => (
          <div
            key={item.id}
            role={item.variant === 'error' ? 'alert' : 'status'}
            data-leaving={item.leaving || undefined}
            className="pointer-events-auto flex w-full items-start gap-3 rounded-xl border bg-popover p-3 text-sm text-popover-foreground shadow-md duration-200 animate-in fade-in slide-in-from-bottom-2 data-leaving:animate-out data-leaving:fade-out data-leaving:slide-out-to-right-4 data-leaving:fill-mode-forwards"
          >
            {item.variant === 'error' ? (
              <CircleAlert aria-hidden className="mt-0.5 size-4 shrink-0 text-destructive" />
            ) : (
              <CheckCircle2 aria-hidden className={cn('mt-0.5 size-4 shrink-0', item.variant === 'info' ? 'text-primary' : 'text-success')} />
            )}
            <div className="min-w-0 flex-1">
              <p className="font-medium">
                {item.title}
              </p>
              {item.description && <p className="text-muted-foreground">{item.description}</p>}
            </div>
            <button
              type="button"
              aria-label="Dismiss notification"
              onClick={() => dismiss(item.id)}
              className="rounded p-0.5 text-muted-foreground hover:text-foreground"
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  return useContext(ToastContext)
}
