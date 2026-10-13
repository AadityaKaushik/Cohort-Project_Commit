import type { MouseEvent } from 'react'
import { Link, useNavigate, type LinkProps } from 'react-router-dom'
import { prefetchRoute } from '@/lib/route-chunks'
import { runRouteTransition } from '@/lib/route-transition'

type TransitionLinkProps = Omit<LinkProps, 'to'> & { to: string }

function isPlainClick(event: MouseEvent<HTMLAnchorElement>): boolean {
  return event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey
}

/** A link that prefetches its route on intent and navigates inside a shared route transition. */
export function TransitionLink({ to, onClick, onMouseEnter, onFocus, onTouchStart, replace, state, ...props }: TransitionLinkProps) {
  const navigate = useNavigate()
  return (
    <Link
      to={to}
      replace={replace}
      state={state}
      onMouseEnter={(event) => {
        prefetchRoute(to)
        onMouseEnter?.(event)
      }}
      onFocus={(event) => {
        prefetchRoute(to)
        onFocus?.(event)
      }}
      onTouchStart={(event) => {
        prefetchRoute(to)
        onTouchStart?.(event)
      }}
      onClick={(event) => {
        onClick?.(event)
        if (event.defaultPrevented || !isPlainClick(event)) {
          return
        }
        event.preventDefault()
        runRouteTransition(() => navigate(to, { replace, state }))
      }}
      {...props}
    />
  )
}
