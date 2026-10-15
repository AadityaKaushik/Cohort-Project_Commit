import { useQuery } from '@tanstack/react-query'
import { AlertTriangleIcon, ExternalLinkIcon } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { directoryKeys, getDirectoryCompany, type DirectoryPay, type DirectorySource } from '@/lib/directory-api'
import { formatLabel } from '@/lib/labels'
import { listTopics } from '@/lib/prep-api'
import { DIRECTORY_NOTE } from '@/pages/directory/DirectoryPage'

const PAY_NOTE = 'Figures are approximate, from public sources, and change with year and level. Not an offer or a guarantee.'

function SourceList({ sources, checked }: { sources: DirectorySource[]; checked: string }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">Sources, last checked {checked}</p>
      <ul className="space-y-1">
        {sources.map((source) => (
          <li key={source.url}>
            <a
              href={source.url}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-xs text-primary hover:underline"
            >
              {source.title} <ExternalLinkIcon aria-hidden className="size-3" />
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}

function payText(pay: DirectoryPay): string {
  const year = pay.year ? `, ${pay.year}` : ', year not stated'
  return `${pay.currency} ${pay.min} to ${pay.max} ${pay.unit} (${pay.level}${year})`
}

/** US-96: one company's sourced interview patterns. */
export function DirectoryProfilePage() {
  const { slug = '' } = useParams()
  const query = useQuery({ queryKey: directoryKeys.detail(slug), queryFn: () => getDirectoryCompany(slug), enabled: slug !== '' })
  const topics = useQuery({ queryKey: ['topics'], queryFn: listTopics })

  if (query.isLoading) {
    return <LoadingRows rows={5} label="Loading company" />
  }
  if (query.isError || !query.data) {
    return (
      <EmptyState
        compact
        icon={AlertTriangleIcon}
        title="Company not found"
        description="This company is not in the directory."
        action={
          <Button variant="outline" asChild>
            <Link to="/directory">Back to the directory</Link>
          </Button>
        }
      />
    )
  }
  const company = query.data
  const topicBySlug = new Map((topics.data ?? []).map((topic) => [topic.slug, topic]))
  const facts = [
    company.industry && `Industry: ${company.industry}`,
    company.locations && `Locations: ${company.locations}`,
    company.size_band && `Size: ${company.size_band}`,
  ].filter((fact): fact is string => Boolean(fact))

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div>
        <h2 className="text-lg font-semibold">{company.name}</h2>
        <p className="flex flex-wrap items-center gap-x-3 text-sm text-muted-foreground">
          {facts.map((fact) => (
            <span key={fact}>{fact}</span>
          ))}
          {company.website && (
            <a href={company.website} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-primary hover:underline">
              Careers page <ExternalLinkIcon aria-hidden className="size-3" />
            </a>
          )}
        </p>
        {company.summary && <p className="mt-2 text-sm">{company.summary}</p>}
      </div>

      <p role="note" className="rounded-lg border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
        {DIRECTORY_NOTE}
        {company.community_rating === null && ` No rating yet (needs ${company.community_min_contributors} candidates).`}
      </p>

      {company.role_patterns.length === 0 && (
        <p className="text-sm text-muted-foreground">No role pattern has been added for this company.</p>
      )}

      {company.role_patterns.map((pattern) => (
        <Card key={`${pattern.role_family}-${pattern.level}`}>
          <CardHeader>
            <CardTitle className="text-sm">
              {formatLabel(pattern.role_family)} · {pattern.level}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ol className="space-y-3">
              {pattern.rounds.map((round, index) => (
                <li key={`${round.name}-${index}`} className="text-sm">
                  <p className="font-medium">
                    {index + 1}. {round.name}
                    {round.duration && <span className="font-normal text-muted-foreground"> · {round.duration}</span>}
                  </p>
                  {round.format && <p className="text-xs text-muted-foreground">{round.format}</p>}
                  {round.tests && <p className="text-sm">Tests: {round.tests}</p>}
                </li>
              ))}
            </ol>

            {pattern.experience_summary && (
              <div className="space-y-1">
                <p className="text-xs font-medium">What candidates commonly report</p>
                <p className="text-sm">{pattern.experience_summary}</p>
              </div>
            )}

            {pattern.notes && <p className="text-xs text-muted-foreground">{pattern.notes}</p>}

            <div className="space-y-1">
              <p className="text-xs font-medium">Expected pay</p>
              {pattern.pay ? (
                <>
                  <p className="text-sm">{payText(pattern.pay)}</p>
                  {pattern.pay.note && <p className="text-xs text-muted-foreground">{pattern.pay.note}</p>}
                </>
              ) : (
                <p className="text-sm text-muted-foreground">No sourced figure yet</p>
              )}
              <p className="text-xs text-muted-foreground">{PAY_NOTE}</p>
            </div>

            {pattern.topic_slugs.length > 0 && (
              <div className="space-y-1">
                <p className="text-xs font-medium">Question bank topics</p>
                <ul className="flex flex-wrap gap-2">
                  {pattern.topic_slugs.map((topicSlug) => {
                    const topic = topicBySlug.get(topicSlug)
                    return (
                      <li key={topicSlug}>
                        {topic ? (
                          <Link
                            to={`/prep/questions?topic_id=${topic.id}`}
                            className="rounded-md border px-2 py-0.5 text-xs hover:bg-accent/40"
                          >
                            {topic.name}
                          </Link>
                        ) : (
                          <span className="rounded-md border px-2 py-0.5 text-xs text-muted-foreground">{formatLabel(topicSlug)}</span>
                        )}
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}

            <SourceList sources={pattern.sources} checked={pattern.last_checked} />
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
