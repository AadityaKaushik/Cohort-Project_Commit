import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeftIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PageHeaderProps {
  title: ReactNode
  description?: ReactNode
  eyebrow?: ReactNode
  back?: { to: string; label: string }
  actions?: ReactNode
  meta?: ReactNode
  className?: string
  asH1?: boolean
}

export function PageHeader({ title, description, eyebrow, back, actions, meta, className, asH1 = false }: PageHeaderProps) {
  const Heading = asH1 ? 'h1' : 'h2'
  return (
    <header className={cn('flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between', className)}>
      <div className="min-w-0 space-y-1.5">
        {back && (
          <Link
            to={back.to}
            className="group/back -ml-1 inline-flex items-center gap-1 rounded-md px-1 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeftIcon aria-hidden className="size-3.5 transition-transform group-hover/back:-translate-x-0.5" />
            {back.label}
          </Link>
        )}
        {eyebrow && !back && <p className="eyebrow">{eyebrow}</p>}
        <Heading className="text-display text-[1.75rem] text-balance break-words sm:text-[2rem]">{title}</Heading>
        {description && (
          <p className="max-w-2xl text-sm text-pretty text-muted-foreground">
            {description}
          </p>
        )}
        {meta && <div className="flex flex-wrap items-center gap-2 pt-1">{meta}</div>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </header>
  )
}
