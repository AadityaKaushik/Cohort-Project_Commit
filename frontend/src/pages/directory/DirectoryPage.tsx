import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AlertTriangleIcon, BuildingIcon, ChevronRightIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { directoryKeys, listDirectoryCompanies } from '@/lib/directory-api'
import { formatLabel } from '@/lib/labels'

export const DIRECTORY_NOTE = 'General pattern, may be out of date. Ratings appear when 5 or more candidates have shared.'

/** US-96: sourced general interview patterns, separate from the companies the user has applied to. */
export function DirectoryPage() {
  const [search, setSearch] = useState('')
  const [family, setFamily] = useState('')
  const query = useQuery({ queryKey: directoryKeys.list(search), queryFn: () => listDirectoryCompanies(search) })
  const all = query.data ?? []
  const families = [...new Set(all.flatMap((company) => company.role_families))].sort()
  const companies = family ? all.filter((company) => company.role_families.includes(family)) : all

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Company directory</h2>
          <p className="text-sm text-muted-foreground">{DIRECTORY_NOTE}</p>
        </div>
        <div className="flex w-full flex-wrap gap-2 sm:w-auto">
          <Input
            aria-label="Search directory"
            placeholder="Search companies"
            className="w-full sm:w-56"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <NativeSelect
            aria-label="Role family"
            className="w-full sm:w-48"
            value={family}
            onChange={(event) => setFamily(event.target.value)}
          >
            <option value="">All role families</option>
            {families.map((value) => (
              <option key={value} value={value}>
                {formatLabel(value)}
              </option>
            ))}
          </NativeSelect>
        </div>
      </div>

      {query.isLoading ? (
        <LoadingRows rows={4} label="Loading directory" />
      ) : query.isError ? (
        <EmptyState
          compact
          icon={AlertTriangleIcon}
          title="Couldn't load the directory"
          description="Something went wrong while fetching companies."
          action={<Button variant="outline" onClick={() => query.refetch()}>Try again</Button>}
        />
      ) : companies.length === 0 ? (
        <EmptyState
          compact
          icon={BuildingIcon}
          title="No companies match"
          description="Only companies with a sourced interview pattern are listed."
        />
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {companies.map((company) => (
            <Link
              key={company.slug}
              to={`/directory/${company.slug}`}
              className="group block rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <Card className="py-0 transition-colors group-hover:bg-accent/40">
                <CardContent className="flex items-center gap-3 p-4">
                  <div className="min-w-0 flex-1 space-y-1">
                    <h3 className="truncate text-sm font-semibold">{company.name}</h3>
                    <p className="truncate text-xs text-muted-foreground">
                      {company.role_families.map(formatLabel).join(', ') || 'No role pattern yet'}
                    </p>
                    <p className="text-xs text-muted-foreground">Checked {company.last_checked}</p>
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
