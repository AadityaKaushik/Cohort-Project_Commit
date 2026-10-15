import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { RepeatIcon } from 'lucide-react'
import { getReviewQueue } from '@/lib/prep-api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { SectionCard } from '@/components/shared/SectionCard'

const VISIBLE_LIMIT = 5

export function ReviewTodayCard({ limit = VISIBLE_LIMIT }: { limit?: number }) {
  const { data, isLoading, isError } = useQuery({ queryKey: ['review-queue'], queryFn: getReviewQueue })

  if (isError) {
    return null
  }

  const items = data?.items ?? []

  return (
    <SectionCard
      title="Review today"
      description="Topics that are due again, based on when you last practised them."
      action={
        items.length > limit ? (
          <Link to="/prep" className="text-sm font-medium text-primary underline-offset-4 hover:underline">
            See all {items.length}
          </Link>
        ) : undefined
      }
    >
      {isLoading && <Skeleton className="h-24 w-full rounded-lg" data-testid="review-loading" />}
      {data && items.length === 0 && (
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <RepeatIcon aria-hidden className="size-4" />
          Nothing to review today. Topics show up here as they come due.
        </p>
      )}
      {items.length > 0 && (
        <ul aria-label="Review queue" className="divide-y overflow-hidden rounded-lg border">
          {items.slice(0, limit).map((item) => (
            <li key={item.topic_id} className="flex items-center justify-between gap-3 px-3 py-2.5">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{item.topic_name}</p>
                <p className="truncate text-xs text-muted-foreground">{item.reason}</p>
              </div>
              <Button variant="outline" size="sm" asChild>
                <Link
                  to={`/prep/questions?topic_id=${item.topic_id}&practise=${item.question_id}`}
                  aria-label={`Practise ${item.topic_name}`}
                >
                  Practise
                </Link>
              </Button>
            </li>
          ))}
        </ul>
      )}
    </SectionCard>
  )
}
