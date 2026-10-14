import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getReadiness } from '@/lib/prep-api'
import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'
import { SectionCard } from '@/components/shared/SectionCard'
import { Skeleton } from '@/components/ui/skeleton'

export function ReadinessCard({ applicationId }: { applicationId: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['readiness', applicationId],
    queryFn: () => getReadiness(applicationId),
  })

  if (isError) {
    return null
  }

  return (
    <SectionCard
      title="Readiness"
      description="Topics named in the job posting, weighted by your mastery."
      action={data && <SampleSizeBadge sampleSize={data.sample_size} sufficientData={data.sufficient_data} />}
    >
      {isLoading && <Skeleton className="h-16 w-full rounded-lg" data-testid="readiness-loading" />}
      {data && data.topics.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No known topics found in this posting. Paste the job description into the application to get a score.
        </p>
      )}
      {data && data.topics.length > 0 && (
        <div className="space-y-3">
          {data.sufficient_data && data.score !== null ? (
            <p className="text-3xl font-semibold tracking-tight">{data.formatted}</p>
          ) : (
            <p className="text-sm text-muted-foreground">
              Not enough practice on these topics yet. Log attempts on at least two of them to see a score.
            </p>
          )}
          <ul className="space-y-1.5 text-sm">
            {data.topics.map((topic) => (
              <li key={topic.topic_id} className="flex items-center justify-between gap-3">
                <span className="truncate">{topic.topic_name}</span>
                <span className="tabular-nums text-muted-foreground">
                  {topic.has_evidence ? `${Math.round(topic.mastery_score * 100)}%` : 'no practice'}
                </span>
              </li>
            ))}
          </ul>
          <Link to="/prep" className="text-sm text-primary underline-offset-4 hover:underline">
            Practise these topics
          </Link>
        </div>
      )}
    </SectionCard>
  )
}
