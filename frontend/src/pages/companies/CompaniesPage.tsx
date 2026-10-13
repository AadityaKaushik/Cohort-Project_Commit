import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { formatDistanceToNow } from 'date-fns'
import { AlertTriangleIcon, Building2Icon, ChevronRightIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { companyKeys, listCompanies } from '@/lib/companies-api'

/** US-96: every company the user has applied to, with a rollup of what they hold for each. */
export function CompaniesPage() {
  const [search, setSearch] = useState('')
  const query = useQuery({ queryKey: companyKeys.list(search), queryFn: () => listCompanies(search) })
  const companies = query.data ?? []

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Companies</h2>
          <p className="text-sm text-muted-foreground">Applications, contacts and notes, grouped by company.</p>
        </div>
        <Input
          aria-label="Search companies"
          placeholder="Search companies"
          className="w-full sm:w-64"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {query.isLoading ? (
        <LoadingRows rows={4} label="Loading companies" />
      ) : query.isError ? (
        <EmptyState
          compact
          icon={AlertTriangleIcon}
          title="Couldn't load companies"
          description="Something went wrong while fetching your companies."
          action={<Button variant="outline" onClick={() => query.refetch()}>Try again</Button>}
        />
      ) : companies.length === 0 ? (
        <EmptyState
          compact
          icon={Building2Icon}
          title={search ? 'No companies match that search' : 'No companies yet'}
          description="Companies appear here as soon as you add an application."
          action={
            search ? undefined : (
              <Button asChild>
                <Link to="/applications/new">Add an application</Link>
              </Button>
            )
          }
        />
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {companies.map((company) => (
            <Link key={company.id} to={`/companies/${company.id}`} className="group block rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
              <Card className="py-0 transition-colors group-hover:bg-accent/40">
                <CardContent className="flex items-center gap-3 p-4">
                  <div className="min-w-0 flex-1 space-y-1">
                    <h3 className="truncate text-sm font-semibold">{company.name}</h3>
                    <p className="truncate text-xs text-muted-foreground">
                      {company.industry ?? 'Industry not set'}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {company.application_count} application{company.application_count === 1 ? '' : 's'}
                      {' · '}
                      {company.active_application_count} active
                      {' · '}
                      {company.contact_count} contact{company.contact_count === 1 ? '' : 's'}
                    </p>
                    {company.last_activity_at && (
                      <p className="text-xs text-muted-foreground">
                        Active {formatDistanceToNow(new Date(company.last_activity_at), { addSuffix: true })}
                      </p>
                    )}
                  </div>
                  <ChevronRightIcon aria-hidden className="size-4 text-muted-foreground" />
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
