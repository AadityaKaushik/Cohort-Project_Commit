import { AlertCircleIcon, SparklesIcon } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acceptRecommendation,
  dismissRecommendation,
  listRecommendations,
  snoozeRecommendation,
} from '@/lib/recommendations-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import { RecommendationCard } from '@/components/recommendations/RecommendationCard'

/** US-51..56,77: the ranked recommendations list, each card carrying its own evidence tier and accept/snooze/dismiss actions. */
export function RecommendationsPage() {
  const queryClient = useQueryClient()

  const recommendationsQuery = useQuery({ queryKey: ['recommendations'], queryFn: listRecommendations })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['recommendations'] })
  }

  const acceptMutation = useMutation({ mutationFn: (id: string) => acceptRecommendation(id), onSuccess: invalidate })
  const snoozeMutation = useMutation({ mutationFn: (id: string) => snoozeRecommendation(id), onSuccess: invalidate })
  const dismissMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => dismissRecommendation(id, reason),
    onSuccess: invalidate,
  })

  const isBusy = acceptMutation.isPending || snoozeMutation.isPending || dismissMutation.isPending

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Recommendations</h2>
        <p className="text-sm text-muted-foreground">
          Ranked suggestions for what to work on next, each with the evidence behind it and how confident it is.
        </p>
      </div>

      {recommendationsQuery.isLoading && (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-32 w-full" />
          ))}
        </div>
      )}

      {recommendationsQuery.isError && (
        <Alert variant="destructive">
          <AlertCircleIcon className="h-4 w-4" />
          <AlertTitle>Couldn&apos;t load recommendations</AlertTitle>
          <AlertDescription>Something went wrong talking to the server.</AlertDescription>
        </Alert>
      )}

      {recommendationsQuery.data && recommendationsQuery.data.length === 0 && (
        <EmptyState
          icon={SparklesIcon}
          title="Nothing to recommend right now"
          description="Log some practice attempts or interview feedback, and targeted recommendations will start showing up here."
        />
      )}

      {recommendationsQuery.data && recommendationsQuery.data.length > 0 && (
        <div className="space-y-4">
          {recommendationsQuery.data.map((recommendation) => (
            <RecommendationCard
              key={recommendation.id}
              recommendation={recommendation}
              isBusy={isBusy}
              onAccept={(id) => acceptMutation.mutate(id)}
              onSnooze={(id) => snoozeMutation.mutate(id)}
              onDismiss={(id, reason) => dismissMutation.mutate({ id, reason })}
            />
          ))}
        </div>
      )}
    </div>
  )
}
