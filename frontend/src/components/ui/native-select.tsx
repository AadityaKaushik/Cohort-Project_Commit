import * as React from 'react'
import { cn } from '@/lib/utils'

function NativeSelect({ className, ...props }: React.ComponentProps<'select'>) {
  return (
    <select
      data-slot="native-select"
      className={cn(
        'h-8 max-sm:h-10 w-full min-w-0 rounded-lg border border-input bg-transparent px-2 text-sm outline-none transition-[color,background-color,border-color,box-shadow] duration-200 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-background/60',
        className,
      )}
      {...props}
    />
  )
}

export { NativeSelect }
