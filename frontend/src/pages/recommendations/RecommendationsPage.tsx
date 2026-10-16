import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertCircleIcon, ListChecksIcon } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acceptRecommendation,
  dismissRecommendation,
  listHandledRecommendations,
  listRecommendations,
  restoreRecommendation,
  snoozeRecommendation,
} from '@/lib/recommendations-api'
import { ApiError } from '@/lib/api-client'
import { useToast } from '@/hooks/use-toast'
import { HandledRecommendations } from '@/components/recommendations/HandledRecommendations'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { EmptyState } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { ErrorState, LoadingCards } from '@/components/shared/QueryStates'
import { Button } from '@/components/ui/button'
import { RecommendationCard } from '@/components/recommendations/RecommendationCard'
import { FederatedLiftToggle } from '@/components/recommendations/FederatedLiftToggle'

export function RecommendationsPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [federated, setFederated] = useState(false)

  const recommendationsQuery = useQuery({
    queryKey: ['recommendations', federated],
    queryFn: () => listRecommendations(federated),
  })

  const handledQuery = useQuery({ queryKey: ['recommendations-handled'], queryFn: listHandledRecommendations })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['recommendations'] })
    queryClient.invalidateQueries({ queryKey: ['recommendations-handled'] })
  }

  function failed(title: string) {
    return (error: unknown) =>
      toast({
        title,
        description: error instanceof ApiError ? error.message : 'Please try again.',
        variant: 'error',
      })
  }

  const acceptMutation = useMutation({
    mutationFn: (id: string) => acceptRecommendation(id),
    onSuccess: (result) => {
      invalidate()
      queryClient.invalidateQueries({ queryKey: ['preparation-tasks'] })
      toast({
        title: result.accepted_task_id ? 'Added to your preparation plan' : 'Marked as handled',
        description: result.accepted_task_id ? 'Find it in your preparation tasks.' : undefined,
      })
    },
    onError: failed('Could not accept the recommendation'),
  })
  const snoozeMutation = useMutation({
    mutationFn: (id: string) => snoozeRecommendation(id),
    onSuccess: () => {
      invalidate()
      toast({ title: 'Snoozed for 7 days', description: 'Find it under Handled if you want it back sooner.' })
    },
    onError: failed('Could not snooze the recommendation'),
  })
  const dismissMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => dismissRecommendation(id, reason),
    onSuccess: () => {
      invalidate()
      toast({ title: 'Dismissed', description: 'You can bring it back from Handled.' })
    },
    onError: failed('Could not dismiss the recommendation'),
  })
  const restoreMutation = useMutation({
    mutationFn: (id: string) => restoreRecommendation(id),
    onSuccess: () => {
      invalidate()
      toast({ title: 'Brought back' })
    },
    onError: failed('Could not bring it back'),
  })

  const isBusy =
    acceptMutation.isPending || snoozeMutation.isPending || dismissMutation.isPending || restoreMutation.isPending

  return (
    <div className="space-y-6">
      <PageHeader
        title="Recommendations"
        description="Suggestions with their evidence."
        actions={
          <>
            <PageGuide guideId="recommendations" />
            <span data-guide="lift">
              <FederatedLiftToggle federated={federated} onChange={setFederated} disabled={recommendationsQuery.isFetching} />
            </span>
          </>
        }
      />

      {recommendationsQuery.isLoading && <LoadingCards count={3} label="Loading recommendations" />}

      {recommendationsQuery.isError && <ErrorState title="Couldn't load recommendations" error={recommendationsQuery.error} onRetry={() => recommendationsQuery.refetch()} />}

      {recommendationsQuery.data && recommendationsQuery.data.length === 0 && (
        <EmptyState
          icon={ListChecksIcon}
          title="No recommendations yet"
          description="Log practice or interview feedback."
          action={
            <Button asChild>
              <Link to="/prep/questions">Practise a question</Link>
            </Button>
          }
          secondaryAction={
            <Button variant="outline" asChild>
              <Link to="/interviews">Debrief an interview</Link>
            </Button>
          }
        />
      )}

      {federated &&
        recommendationsQuery.data &&
        recommendationsQuery.data.length > 0 &&
        !recommendationsQuery.data[0].federated_applied && (
          <Alert>
            <AlertCircleIcon className="h-4 w-4" />
            <AlertTitle>Shared model unavailable</AlertTitle>
            <AlertDescription>Showing your local-only ranking instead. Nothing about your data changed.</AlertDescription>
          </Alert>
        )}

      {recommendationsQuery.data && recommendationsQuery.data.length > 0 && (
        <div data-guide="cards" className="space-y-4">
          {recommendationsQuery.data.map((recommendation) => (
            <RecommendationCard
              key={recommendation.id}
              recommendation={recommendation}
              isBusy={isBusy}
              showRankDelta={federated}
              onAccept={(id) => acceptMutation.mutate(id)}
              onSnooze={(id) => snoozeMutation.mutate(id)}
              onDismiss={(id, reason) => dismissMutation.mutate({ id, reason })}
            />
          ))}
        </div>
      )}

      {handledQuery.data && handledQuery.data.length > 0 && (
        <section aria-label="Handled recommendations" className="space-y-3">
          <h3 className="font-heading text-base font-semibold tracking-tight">Handled</h3>
          <HandledRecommendations items={handledQuery.data} isBusy={isBusy} onRestore={(id) => restoreMutation.mutate(id)} />
        </section>
      )}
    </div>
  )
}
