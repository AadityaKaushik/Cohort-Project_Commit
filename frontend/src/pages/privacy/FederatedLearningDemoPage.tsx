import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, PlayIcon, RotateCcwIcon } from 'lucide-react'
import { format, parseISO } from 'date-fns'
import { FlAccuracyChart } from '@/components/privacy/FlAccuracyChart'
import { FlWeightsChart } from '@/components/privacy/FlWeightsChart'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingCards } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { ApiError } from '@/lib/api-client'
import {
  getFlDemoStatus,
  resetFlDemo,
  runFlDemoRound,
  setSyntheticClients,
  type FlDemoRoundResult,
  type FlDemoStatus,
} from '@/lib/fl-demo-api'
import { cn } from '@/lib/utils'

const CLIENT_OPTIONS = [0, 1, 5, 20, 40]
const STATUS_KEY = ['fl-demo-status']

const STEPS = [
  'Every opted-in person, and every synthetic one, trains the current shared model on their own rated interview rounds.',
  'Only the change they made leaves them: clipped so no one person dominates, then noised so it cannot be traced back.',
  'The server refuses the round if fewer than 3 people took part, and drops any update far larger than a clipped one can be.',
  'It averages what is left and publishes a new model version.',
  'Recommendations use the newest version to predict how confident you will feel in your next round.',
]

export function FederatedLearningDemoPage() {
  const queryClient = useQueryClient()
  const statusQuery = useQuery({ queryKey: STATUS_KEY, queryFn: getFlDemoStatus, retry: false })
  const [noise, setNoise] = useState(1)
  const [lastResult, setLastResult] = useState<FlDemoRoundResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  const refresh = () => queryClient.invalidateQueries({ queryKey: STATUS_KEY })
  const onError = (err: unknown) => setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')

  const clientsMutation = useMutation({ mutationFn: (count: number) => setSyntheticClients(count), onSuccess: refresh, onError })
  const resetMutation = useMutation({
    mutationFn: resetFlDemo,
    onSuccess: () => {
      setLastResult(null)
      refresh()
    },
    onError,
  })
  const roundMutation = useMutation({
    mutationFn: async (rounds: number) => {
      let result: FlDemoRoundResult | null = null
      for (let i = 0; i < rounds; i++) {
        result = await runFlDemoRound(noise)
      }
      return result
    },
    onMutate: () => setError(null),
    onSuccess: (result) => {
      setLastResult(result)
      refresh()
    },
    onError,
  })
  const busy = clientsMutation.isPending || resetMutation.isPending || roundMutation.isPending

  if (statusQuery.isLoading) {
    return <LoadingCards count={3} label="Loading the federated learning demo" />
  }
  if (statusQuery.isError) {
    const notDev = statusQuery.error instanceof ApiError && statusQuery.error.status === 404
    return (
      <div className="space-y-6">
        <PageHeader title="Federated learning demo" />
        <ErrorState
          title={notDev ? 'This page only exists in the dev profile' : 'Could not load the demo'}
          description={notDev ? 'Start the API with SPRING_PROFILES_ACTIVE=dev to use it.' : 'Try again in a moment.'}
          onRetry={notDev ? undefined : () => statusQuery.refetch()}
        />
      </div>
    )
  }
  const status = statusQuery.data as FlDemoStatus

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Dev demo"
        title="Federated learning"
        description="The shared model, trained round by round without anyone's data leaving their own account. Synthetic people fill the group so a round can run with only you."
        actions={
          <Button variant="outline" asChild>
            <Link to="/privacy">Privacy Ledger</Link>
          </Button>
        }
      />

      {!status.available && (
        <ErrorState
          title="The federated learning service is not running"
          description="Start it with: docker compose -f docker-compose.yml -f docker-compose.fl.yml up --build"
          onRetry={() => statusQuery.refetch()}
        />
      )}

      {status.available && status.model && (
        <>
          <section aria-label="Current model" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Figure label="Model version" value={`v${status.model.version}`} hint={status.model.version === 0 ? 'Not trained yet' : `From round ${status.model.round_number}, ${status.model.participant_count} people`} />
            <Figure label="Check set accuracy" value={percent(status.model.check_accuracy)} hint={`Coin flip is 50%. All data in one place reaches ${percent(status.central_ceiling)}.`} />
            <Figure label="People in a round" value={String(status.synthetic_clients + (status.opted_in ? 1 : 0))} hint={`${status.synthetic_clients} synthetic${status.opted_in ? ' and you' : ''}`} />
            <Figure
              label="Your next round"
              value={percent(status.your_predicted_confidence)}
              hint="The model's chance you will rate it 4 or 5 for confidence"
            />
          </section>

          <SectionCard title="Run a round" description="Pick how many synthetic people join, and how much noise each update gets, then run one round or five.">
            <div className="flex flex-wrap items-end gap-4">
              <label className="flex flex-col gap-1.5 text-sm">
                <span className="font-medium">Synthetic people</span>
                <NativeSelect
                  aria-label="Synthetic people"
                  className="w-32"
                  value={String(status.synthetic_clients)}
                  disabled={busy}
                  onChange={(event) => clientsMutation.mutate(Number(event.target.value))}
                >
                  {[...new Set([...CLIENT_OPTIONS, status.synthetic_clients])].sort((a, b) => a - b).map((count) => (
                    <option key={count} value={count}>
                      {count}
                    </option>
                  ))}
                </NativeSelect>
              </label>
              <label className="flex flex-col gap-1.5 text-sm">
                <span className="font-medium">Noise</span>
                <NativeSelect aria-label="Noise" className="w-44" value={String(noise)} disabled={busy} onChange={(event) => setNoise(Number(event.target.value))}>
                  {status.noise_levels.map((level) => (
                    <option key={level} value={level}>
                      {level === 1 ? '1.0 (normal)' : `${level} (demo only)`}
                    </option>
                  ))}
                </NativeSelect>
              </label>
              <div className="flex flex-wrap gap-2">
                <Button variant="cta" disabled={busy} onClick={() => roundMutation.mutate(1)}>
                  {roundMutation.isPending ? <Loader2 aria-hidden className="animate-spin" /> : <PlayIcon aria-hidden />}
                  Run a round
                </Button>
                <Button variant="outline" disabled={busy} onClick={() => roundMutation.mutate(5)}>
                  Run 5 rounds
                </Button>
                <Button variant="ghost" disabled={busy} onClick={() => resetMutation.mutate()}>
                  <RotateCcwIcon aria-hidden />
                  Reset to version 0
                </Button>
              </div>
            </div>
            <p className="mt-3 text-xs text-muted-foreground">
              {status.opted_in
                ? status.your_rated_rounds >= status.minimum_rated_rounds
                  ? `You are opted in with ${status.your_rated_rounds} rated interview rounds, so you train in every round too.`
                  : `You are opted in but have ${status.your_rated_rounds} rated interview rounds; ${status.minimum_rated_rounds} are needed to train, so you sit out.`
                : 'You are not opted in, so only synthetic people train. Opt in on the Privacy page to take part.'}{' '}
              Pick 1 synthetic person to see a round refused for being too small.
            </p>
            {error && (
              <p role="alert" className="mt-3 text-sm text-destructive">
                {error}
              </p>
            )}
            {lastResult && <RoundResult result={lastResult} />}
          </SectionCard>

          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
            <SectionCard
              title={`What version ${status.model.version} learned`}
              description="Each bar is one of the 24 shared features. Hover a bar for its exact weight."
            >
              <FlWeightsChart weights={status.model.weights} />
            </SectionCard>
            <div className="space-y-6">
              <SectionCard title="Accuracy per version" description="Scored on a fixed synthetic check set that no client trains on.">
                <FlAccuracyChart versions={status.versions} ceiling={status.central_ceiling} />
              </SectionCard>
              <SectionCard title="What happens in a round">
                <ol className="space-y-2.5 text-sm">
                  {STEPS.map((step, index) => (
                    <li key={step} className="flex gap-3">
                      <span className="tabular shrink-0 text-muted-foreground">{index + 1}</span>
                      {step}
                    </li>
                  ))}
                </ol>
              </SectionCard>
            </div>
          </div>
        </>
      )}

      <SectionCard title="Recent rounds" description="Stored by the API. Accepted rounds publish a model version; refused rounds leave the last one serving.">
        {status.rounds.length === 0 ? (
          <p className="text-sm text-muted-foreground">No rounds yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[40rem] text-sm">
              <thead>
                <tr className="board-label border-b-2 border-rule text-left text-muted-foreground">
                  <th className="py-2 pr-3 font-medium">Round</th>
                  <th className="py-2 pr-3 font-medium">Time</th>
                  <th className="py-2 pr-3 font-medium">Result</th>
                  <th className="py-2 pr-3 font-medium">Trained</th>
                  <th className="py-2 pr-3 font-medium">Sat out</th>
                  <th className="py-2 pr-3 font-medium">Noise</th>
                  <th className="py-2 pr-3 font-medium">Model</th>
                  <th className="py-2 font-medium">Accuracy</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {status.rounds.map((round) => (
                  <tr key={`${round.round_number}-${round.created_at}`} className="align-top">
                    <td className="tabular py-2 pr-3">{round.round_number}</td>
                    <td className="tabular py-2 pr-3 text-muted-foreground">{format(parseISO(round.created_at), 'HH:mm:ss')}</td>
                    <td className="py-2 pr-3">
                      <span className={cn('font-medium', round.accepted ? 'text-success' : 'text-destructive')}>{round.accepted ? 'Accepted' : 'Refused'}</span>
                      {!round.accepted && round.reason && <span className="block max-w-xs text-xs text-muted-foreground">{shortReason(round.reason)}</span>}
                    </td>
                    <td className="tabular py-2 pr-3">{round.cohort_size}</td>
                    <td className="tabular py-2 pr-3">{round.sat_out_count}</td>
                    <td className="tabular py-2 pr-3">{round.noise_multiplier ?? '-'}</td>
                    <td className="tabular py-2 pr-3">{round.model_version === null ? '-' : `v${round.model_version}`}</td>
                    <td className="tabular py-2">{percent(round.check_accuracy)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>
    </div>
  )
}

function Figure({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="rounded-md border border-t-[3px] border-t-rule bg-card p-4">
      <p className="board-label text-muted-foreground">{label}</p>
      <p className="text-display mt-2 text-4xl">{value}</p>
      <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
    </div>
  )
}

function RoundResult({ result }: { result: FlDemoRoundResult }) {
  if (!result.round_ran) {
    return (
      <p role="status" className="mt-3 rounded-sm bg-destructive/10 px-3 py-2 text-sm">
        {result.export_failures > 0
          ? 'No round ran: the federated learning service could not be reached.'
          : 'No round ran: nobody real is opted in. Opt in on the Privacy page first.'}
      </p>
    )
  }
  return (
    <p role="status" className={cn('mt-3 rounded-sm px-3 py-2 text-sm', result.accepted ? 'bg-accent' : 'bg-destructive/10')}>
      {result.accepted
        ? `Round ${result.round_number} accepted: ${result.cohort_size} people trained${result.sat_out_count ? `, ${result.sat_out_count} sat out` : ''}. The model is now version ${result.model_version}, scoring ${percent(result.check_accuracy)} on the check set.`
        : `Round ${result.round_number} refused: ${shortReason(result.reason ?? '')}`}
    </p>
  )
}

function shortReason(reason: string): string {
  return reason.replace(/^round \d+ refused: /, '')
}

function percent(value: number | null): string {
  return value === null ? '-' : `${Math.round(value * 100)}%`
}
