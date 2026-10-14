import { useQuery } from '@tanstack/react-query'
import { BriefcaseIcon, MessagesSquareIcon, PercentIcon, TrophyIcon } from 'lucide-react'
import { getConversionRates, getFunnel, getRejectionDistribution, getTimeline, type RejectionDistribution } from '@/lib/analytics-api'
import { formatLabel } from '@/lib/labels'
import { Skeleton } from '@/components/ui/skeleton'
import { FunnelChart } from '@/components/analytics/FunnelChart'
import { ConversionCards } from '@/components/analytics/ConversionCards'
import { ActivityTimelineChart } from '@/components/analytics/ActivityTimelineChart'
import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingCards } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { StatGrid, StatTile } from '@/components/shared/StatTile'
import { cn } from '@/lib/utils'

export function AnalyticsPage() {
  const funnelQuery = useQuery({ queryKey: ['analytics-funnel'], queryFn: getFunnel })
  const conversionsQuery = useQuery({ queryKey: ['analytics-conversions'], queryFn: getConversionRates })
  const timelineQuery = useQuery({ queryKey: ['analytics-timeline'], queryFn: () => getTimeline() })
  const rejectionsQuery = useQuery({ queryKey: ['analytics-rejections'], queryFn: getRejectionDistribution })

  const funnel = funnelQuery.data
  const overall = conversionsQuery.data?.overall_success

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics"
        description="Every number here shows how much data it's based on — small samples are flagged, never hidden."
      />

      <StatGrid>
        <StatTile label="Logged applications" icon={BriefcaseIcon} value={funnel?.applied ?? null} loading={funnelQuery.isLoading} hint="Every application you have tracked" />
        <StatTile
          label="Reached interview"
          icon={MessagesSquareIcon}
          tone="info"
          value={funnel?.reached_interview ?? null}
          loading={funnelQuery.isLoading}
          hint="At least one interview round"
        />
        <StatTile label="Offers" icon={TrophyIcon} tone="success" value={funnel?.reached_offer ?? null} loading={funnelQuery.isLoading} hint="Ever reached the offer stage" />
        <StatTile
          label="Success rate"
          icon={PercentIcon}
          tone="warning"
          value={overall && overall.sufficient_data ? overall.value * 100 : null}
          decimals={1}
          suffix="%"
          muted={overall ? !overall.sufficient_data : false}
          loading={conversionsQuery.isLoading}
          hint={overall ? (overall.sufficient_data ? 'From application to offer' : 'Not enough decided applications yet') : undefined}
        />
      </StatGrid>

      <SectionCard title="Application funnel" description="How many applications ever reached each stage.">
        {funnelQuery.isLoading && <Skeleton className="h-[248px] w-full rounded-lg" data-testid="funnel-loading" />}
        {funnelQuery.isError && <ErrorState title="Couldn't load the funnel" onRetry={() => funnelQuery.refetch()} />}
        {funnel && <FunnelChart funnel={funnel} />}
      </SectionCard>

      <section aria-labelledby="conversion-heading" className="space-y-3">
        <h3 id="conversion-heading" className="font-heading text-base font-semibold tracking-tight">
          Conversion rates
        </h3>
        {conversionsQuery.isLoading && (
          <div data-testid="conversions-loading">
            <LoadingCards count={5} className="grid-cols-2 xl:grid-cols-5" label="Loading conversion rates" />
          </div>
        )}
        {conversionsQuery.isError && <ErrorState title="Couldn't load conversion rates" onRetry={() => conversionsQuery.refetch()} />}
        {conversionsQuery.data && <ConversionCards rates={conversionsQuery.data} />}
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <SectionCard title="Activity over time" description="Applications and interviews logged per week.">
          {timelineQuery.isLoading && <Skeleton className="h-[260px] w-full rounded-lg" data-testid="timeline-loading" />}
          {timelineQuery.isError && <ErrorState title="Couldn't load activity" onRetry={() => timelineQuery.refetch()} />}
          {timelineQuery.data && <ActivityTimelineChart timeline={timelineQuery.data} />}
        </SectionCard>

        <SectionCard
          title="Where rejections happen"
          description="The last stage reached before a rejection."
          action={
            rejectionsQuery.data && (
              <SampleSizeBadge sampleSize={rejectionsQuery.data.sample_size} sufficientData={rejectionsQuery.data.sufficient_data} />
            )
          }
        >
          {rejectionsQuery.isLoading && <Skeleton className="h-40 w-full rounded-lg" data-testid="rejections-loading" />}
          {rejectionsQuery.isError && <ErrorState title="Couldn't load rejections" onRetry={() => rejectionsQuery.refetch()} />}
          {rejectionsQuery.data && <RejectionDistributionList distribution={rejectionsQuery.data} />}
        </SectionCard>
      </div>
    </div>
  )
}

function RejectionDistributionList({ distribution }: { distribution: RejectionDistribution }) {
  const stages = Object.entries(distribution.by_stage) as [string, number][]

  if (stages.length === 0 && distribution.no_response_count === 0) {
    return <p className="rounded-lg border border-dashed bg-surface-2/50 p-6 text-center text-sm text-muted-foreground">No rejections recorded yet.</p>
  }

  const rows = [
    ...stages.map(([stage, count]) => ({ key: stage, label: `Rejected after ${formatLabel(stage)}`, count, muted: false })),
    { key: 'no-response', label: 'No response (60+ days)', count: distribution.no_response_count, muted: true },
  ]
  const peak = Math.max(1, ...rows.map((row) => row.count))

  return (
    <ul className="space-y-4">
      {rows.map((row, index) => (
        <li key={row.key} className="space-y-1.5">
          <div className="flex items-center justify-between gap-3 text-sm">
            <span className="text-muted-foreground">{row.label}</span>
            <span className="font-semibold tabular-nums">{row.count}</span>
          </div>
          <div aria-hidden className="h-1.5 overflow-hidden rounded-full bg-muted">
            <span
              className={cn('animate-grow-x block h-full origin-left rounded-full', row.muted ? 'bg-muted-foreground/50' : 'bg-primary')}
              style={{ width: `${(row.count / peak) * 100}%`, animationDelay: `${index * 60}ms` }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}
