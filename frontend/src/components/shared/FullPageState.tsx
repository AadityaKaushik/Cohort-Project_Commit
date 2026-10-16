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
    <div className="flex min-h-screen flex-col items-center justify-center gap-5 bg-background px-6 text-center">
      <BrandMark />
      <Icon aria-hidden className="size-8 text-muted-foreground" />
      <div className="max-w-md space-y-2">
        <p className="text-sm text-muted-foreground">{eyebrow}</p>
        <h1 className="text-2xl font-semibold">{title}</h1>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <div className="flex flex-wrap items-center justify-center gap-2">{actions}</div>
    </div>
  )
}
