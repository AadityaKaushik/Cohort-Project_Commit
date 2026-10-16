import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { BookOpenTextIcon, PlusIcon } from 'lucide-react'
import { COMPETENCIES, firstLine, listStories, storyKeys, type Competency } from '@/lib/stories-api'
import { formatLabel } from '@/lib/labels'
import { Button } from '@/components/ui/button'
import { EmptyState } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingCards } from '@/components/shared/QueryStates'
import { ToneBadge } from '@/components/shared/ToneBadge'
import { cn } from '@/lib/utils'

export function StoriesListPage() {
  const [competency, setCompetency] = useState<Competency | 'all'>('all')
  const query = useQuery({
    queryKey: storyKeys.list(competency),
    queryFn: () => listStories(competency === 'all' ? undefined : competency),
  })
  const stories = query.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Stories"
        description="Situation, task, action and result stories to reuse in behavioural rounds."
        actions={
          <Button asChild>
            <Link to="/stories/new">
              <PlusIcon aria-hidden />
              New story
            </Link>
          </Button>
        }
      />

      <div role="group" aria-label="Filter by competency" className="flex flex-wrap gap-2">
        {(['all', ...COMPETENCIES] as const).map((value) => (
          <button
            key={value}
            type="button"
            aria-pressed={competency === value}
            onClick={() => setCompetency(value)}
            className={cn(
              'rounded-full border px-3 py-1 text-xs font-medium transition-colors',
              competency === value ? 'border-primary bg-primary text-primary-foreground' : 'bg-card hover:bg-accent',
            )}
          >
            {value === 'all' ? 'All' : formatLabel(value)}
          </button>
        ))}
      </div>

      {query.isLoading && <LoadingCards label="Loading stories" />}
      {query.isError && <ErrorState title="Couldn't load your stories" description="Try again in a moment." onRetry={() => query.refetch()} />}
      {query.isSuccess && stories.length === 0 && (
        <EmptyState
          icon={BookOpenTextIcon}
          title={competency === 'all' ? 'No stories yet' : `No ${formatLabel(competency).toLowerCase()} stories`}
          description="Write a story once and reuse it across interviews."
          action={
            <Button asChild>
              <Link to="/stories/new">Add a story</Link>
            </Button>
          }
        />
      )}
      {stories.length > 0 && (
        <ul className="grid gap-4 md:grid-cols-2">
          {stories.map((story) => (
            <li key={story.id}>
              <Link to={`/stories/${story.id}`} className="block h-full space-y-3 rounded-xl border bg-card p-4 transition-colors hover:border-primary/40">
                <h3 className="font-heading text-sm font-semibold">{story.title}</h3>
                {story.competencies.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {story.competencies.map((item) => (
                      <ToneBadge key={item} tone="neutral">
                        {formatLabel(item)}
                      </ToneBadge>
                    ))}
                  </div>
                )}
                {firstLine(story.result) && <p className="line-clamp-2 text-sm text-muted-foreground">{firstLine(story.result)}</p>}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
