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

interface CountUpProps {
  value: number
  decimals?: number
  suffix?: string
  className?: string
}

function CountUpValue({ value, decimals = 0, suffix = '', className }: CountUpProps) {
  const { ref, text } = useCountUp<HTMLSpanElement>(value, decimals, suffix)
  return (
    <span className={cn('tabular-nums', className)}>
      <span aria-hidden ref={ref}>
        {text}
      </span>
      <span className="sr-only">
        {value.toFixed(decimals)}
        {suffix}
      </span>
    </span>
  )
}

export function CountUp(props: CountUpProps) {
  return <CountUpValue key={props.value} {...props} />
}
