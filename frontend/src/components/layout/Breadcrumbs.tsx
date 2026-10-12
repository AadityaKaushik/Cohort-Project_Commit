import { Fragment } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import type { Crumb } from '@/lib/navigation'

export function Breadcrumbs({ crumbs }: { crumbs: Crumb[] }) {
  return (
    <nav aria-label="Breadcrumb" className="min-w-0">
      <ol className="flex items-center gap-1 text-sm">
        {crumbs.map((crumb, index) => {
          const isLast = index === crumbs.length - 1
          return (
            <Fragment key={`${crumb.label}-${index}`}>
              {index > 0 && <ChevronRight aria-hidden className="size-3.5 shrink-0 text-muted-foreground" />}
              <li className="min-w-0 truncate">
                {isLast || !crumb.path ? (
                  <h1 aria-current={isLast ? 'page' : undefined} className="inline text-sm font-medium">
                    {crumb.label}
                  </h1>
                ) : (
                  <Link to={crumb.path} className="text-muted-foreground hover:text-foreground">
                    {crumb.label}
                  </Link>
                )}
              </li>
            </Fragment>
          )
        })}
      </ol>
    </nav>
  )
}
