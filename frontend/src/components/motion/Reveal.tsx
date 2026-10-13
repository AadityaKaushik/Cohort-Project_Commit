import type { CSSProperties, ElementType, ReactNode } from 'react'
import { useCountUp, useInView } from '@/hooks/use-motion'
import { cn } from '@/lib/utils'

interface RevealProps {
  as?: ElementType
  delay?: number
  className?: string
  children: ReactNode
}

export function Reveal({ as: Component = 'div', delay = 0, className, children }: RevealProps) {
  const [ref, inView] = useInView<HTMLElement>()
  const style: CSSProperties | undefined = delay ? { transitionDelay: `${delay}ms` } : undefined

  return (
    <Component ref={ref} data-visible={inView} className={cn('reveal', className)} style={style}>
      {children}
    </Component>
  )
}

function CountUpValue({ value, className }: { value: number; className?: string }) {
  const shown = useCountUp(value)
  return (
    <span className={cn('tabular-nums', className)}>
      <span aria-hidden>{shown}</span>
      <span className="sr-only">{value}</span>
    </span>
  )
}

export function CountUp({ value, className }: { value: number; className?: string }) {
  return <CountUpValue key={value} value={value} className={className} />
}
