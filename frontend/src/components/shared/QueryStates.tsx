import type { ReactNode } from 'react'
import { AlertTriangleIcon, RotateCwIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

export function ErrorState({
  title = 'Something went wrong',
  description = 'We could not load this just now. Check your connection and try again.',
  onRetry,
  action,
  className,
}: {
  title?: string
  description?: string
  onRetry?: () => void
  action?: ReactNode
  className?: string
}) {
  return (
    <div
      role="alert"
      className={cn(
        'animate-enter flex flex-col gap-3 rounded-xl border border-destructive/30 bg-destructive/5 p-4 sm:flex-row sm:items-center sm:gap-4',
        className,
      )}
    >
      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-destructive/10 text-destructive">
        <AlertTriangleIcon aria-hidden className="size-4" />
      </span>
      <div className="min-w-0 flex-1 space-y-0.5">
        <p className="text-sm font-semibold text-destructive">{title}</p>
        <p className="text-sm text-pretty text-muted-foreground">{description}</p>
      </div>
      {(onRetry || action) && (
        <div className="flex shrink-0 gap-2">
          {action}
          {onRetry && (
            <Button variant="outline" size="sm" onClick={onRetry}>
              <RotateCwIcon aria-hidden />
              Try again
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

export function LoadingRows({ rows = 6, label = 'Loading' }: { rows?: number; label?: string }) {
  return (
    <div role="status" aria-label={label} className="surface-raised overflow-hidden rounded-xl border">
      <div className="flex gap-6 border-b bg-surface-2/60 px-4 py-3">
        {[28, 20, 14, 16].map((width, index) => (
          <Skeleton key={index} className="h-3" style={{ width: `${width}%` }} />
        ))}
      </div>
      <div className="divide-y">
        {Array.from({ length: rows }).map((_, index) => (
          <div key={index} className="flex items-center gap-6 px-4 py-3.5">
            <Skeleton className="h-4 w-1/4" />
            <Skeleton className="h-4 w-1/5" />
            <Skeleton className="h-5 w-16 rounded-full" />
            <Skeleton className="ml-auto h-4 w-20" />
          </div>
        ))}
      </div>
    </div>
  )
}

export function LoadingCards({ count = 3, className, label = 'Loading' }: { count?: number; className?: string; label?: string }) {
  return (
    <div role="status" aria-label={label} className={cn('grid gap-3', className)}>
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="surface-raised space-y-3 rounded-xl border p-4 sm:p-5">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-7 w-20" />
          <Skeleton className="h-3 w-2/3" />
        </div>
      ))}
    </div>
  )
}

export function PageSkeleton({ label = 'Loading page', variant = 'detail' }: { label?: string; variant?: 'detail' | 'form' }) {
  return (
    <div role="status" aria-label={label} className="space-y-6">
      <div className="space-y-2">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="h-8 w-72 max-w-full" />
        <Skeleton className="h-4 w-96 max-w-full" />
      </div>
      {variant === 'detail' ? (
        <div className="grid gap-6 lg:grid-cols-3">
          <Skeleton className="h-64 rounded-xl lg:col-span-2" />
          <Skeleton className="h-64 rounded-xl" />
        </div>
      ) : (
        <Skeleton className="h-[28rem] max-w-3xl rounded-xl" />
      )}
    </div>
  )
}
