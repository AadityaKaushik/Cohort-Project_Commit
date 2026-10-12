import type { ReactNode } from 'react'
import { ThemeToggle } from '@/components/layout/ThemeToggle'

export function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <div className="relative flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <ThemeToggle className="absolute top-4 right-4" />
      {children}
    </div>
  )
}
