import type { ElementType, ReactNode } from 'react'
import { BrandMark } from '@/components/brand/BrandMark'

interface FullPageStateProps {
  icon: ElementType
  eyebrow: string
  title: string
  description: string
  actions: ReactNode
}

export function FullPageState({ icon: Icon, eyebrow, title, description, actions }: FullPageStateProps) {
  return (
    <div className="relative isolate flex min-h-screen flex-col items-center justify-center gap-6 overflow-hidden bg-background px-6 text-center">
      <div aria-hidden className="hero-glow absolute inset-x-0 top-0 -z-10 h-96" />
      <div aria-hidden className="hero-grid absolute inset-0 -z-10" />
      <BrandMark />
      <div className="animate-enter flex size-14 items-center justify-center rounded-2xl border bg-card text-primary shadow-[var(--elevation)]">
        <Icon aria-hidden className="size-6" />
      </div>
      <div className="animate-enter max-w-md space-y-2">
        <p className="eyebrow">{eyebrow}</p>
        <h1 className="text-display text-3xl">{title}</h1>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <div className="flex flex-wrap items-center justify-center gap-2">{actions}</div>
    </div>
  )
}
