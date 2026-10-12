import { useQuery } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import { getConversionRates, getFunnel, getRejectionDistribution, getTimeline, type RejectionDistribution } from '@/lib/analytics-api'
import { formatLabel } from '@/lib/labels'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { FunnelChart } from '@/components/analytics/FunnelChart'
import { ConversionCards } from '@/components/analytics/ConversionCards'
import { ActivityTimelineChart } from '@/components/analytics/ActivityTimelineChart'
import { SampleSizeBadge } from '@/components/analytics/SampleSizeBadge'

/** US-43..50: funnel, conversion rates, activity timeline, and rejection distribution -- every number sample-size-gated (FR-67/US-50). */
export function AnalyticsPage() {
  const funnelQuery = useQuery({ queryKey: ['analytics-funnel'], queryFn: getFunnel })
  const conversionsQuery = useQuery({ queryKey: ['analytics-conversions'], queryFn: getConversionRates })
  const timelineQuery = useQuery({ queryKey: ['analytics-timeline'], queryFn: () => getTimeline() })
  const rejectionsQuery = useQuery({ queryKey: ['analytics-rejections'], queryFn: getRejectionDistribution })

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Analytics</h2>
        <p className="text-sm text-muted-foreground">
          Every number here shows how much data it's based on — small samples are flagged, never hidden.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Application funnel</CardTitle>
        </CardHeader>
        <CardContent>
          {funnelQuery.isLoading && <Skeleton className="h-[220px] w-full" data-testid="funnel-loading" />}
          {funnelQuery.isError && <ErrorAlert />}
          {funnelQuery.data && <FunnelChart funnel={funnelQuery.data} />}
        </CardContent>
      </Card>

      <div>
        <h3 className="mb-3 text-sm font-medium text-muted-foreground">Conversion rates</h3>
        {conversionsQuery.isLoading && (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3" data-testid="conversions-loading">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-28 w-full" />
            ))}
          </div>
        )}
        {conversionsQuery.isError && <ErrorAlert />}
        {conversionsQuery.data && <ConversionCards rates={conversionsQuery.data} />}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Activity over time</CardTitle>
        </CardHeader>
        <CardContent>
          {timelineQuery.isLoading && <Skeleton className="h-[240px] w-full" data-testid="timeline-loading" />}
          {timelineQuery.isError && <ErrorAlert />}
          {timelineQuery.data && <ActivityTimelineChart timeline={timelineQuery.data} />}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Rejection stage distribution</CardTitle>
          {rejectionsQuery.data && (
            <SampleSizeBadge sampleSize={rejectionsQuery.data.sample_size} sufficientData={rejectionsQuery.data.sufficient_data} />
          )}
        </CardHeader>
        <CardContent>
          {rejectionsQuery.isLoading && <Skeleton className="h-24 w-full" data-testid="rejections-loading" />}
          {rejectionsQuery.isError && <ErrorAlert />}
          {rejectionsQuery.data && <RejectionDistributionList distribution={rejectionsQuery.data} />}
        </CardContent>
      </Card>
    </div>
  )
}

function RejectionDistributionList({ distribution }: { distribution: RejectionDistribution }) {
  const stages = Object.entries(distribution.by_stage) as [string, number][]

  if (stages.length === 0 && distribution.no_response_count === 0) {
    return <p className="text-sm text-muted-foreground">No rejections recorded yet.</p>
  }

  return (
    <ul className="space-y-1.5 text-sm">
      {stages.map(([stage, count]) => (
        <li key={stage} className="flex items-center justify-between">
          <span className="text-muted-foreground">Rejected after {formatLabel(stage)}</span>
          <span className="font-medium">{count}</span>
        </li>
      ))}
      <li className="flex items-center justify-between border-t pt-1.5">
        <span className="text-muted-foreground">No response (60+ days)</span>
        <span className="font-medium">{distribution.no_response_count}</span>
      </li>
    </ul>
  )
}

function ErrorAlert() {
  return (
    <Alert variant="destructive">
      <AlertCircleIcon className="h-4 w-4" />
      <AlertTitle>Couldn&apos;t load this chart</AlertTitle>
      <AlertDescription>Something went wrong talking to the server.</AlertDescription>
    </Alert>
  )
}
