import { LockIcon, ScrollTextIcon, ShieldCheckIcon, ShieldOffIcon, DownloadIcon, UploadIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getFlConsent, getFlSchema, listPrivacyLedger, optInFl, optOutFl } from '@/lib/fl-api'
import { getFlDemoStatus } from '@/lib/fl-demo-api'
import { PrivacyLedgerTable } from '@/components/privacy/PrivacyLedgerTable'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { ToneBadge } from '@/components/shared/ToneBadge'

const PLAIN_LANGUAGE_POINTS = [
  {
    icon: UploadIcon,
    title: 'What leaves your account',
    body: 'A short list of bounded numbers that describe how your preparation is going, such as an average mastery score. Never your resumes, notes, company names or anything you typed.',
  },
  {
    icon: LockIcon,
    title: 'How it is protected',
    body: 'Before anything is sent, the numbers are capped to a fixed size and have random noise added, so no single person can be picked out of the shared result.',
  },
  {
    icon: DownloadIcon,
    title: 'Benefits',
    body: 'Benchmarks from all participants improve recommendations. You can switch this off at any time.',
  },
]

export function PrivacyPage() {
  const queryClient = useQueryClient()

  const consentQuery = useQuery({ queryKey: ['fl-consent'], queryFn: getFlConsent })
  const schemaQuery = useQuery({ queryKey: ['fl-schema'], queryFn: getFlSchema })
  const ledgerQuery = useQuery({ queryKey: ['fl-privacy-ledger'], queryFn: listPrivacyLedger })

  function invalidateConsent() {
    queryClient.invalidateQueries({ queryKey: ['fl-consent'] })
  }

  const optInMutation = useMutation({ mutationFn: optInFl, onSuccess: invalidateConsent })
  const optOutMutation = useMutation({ mutationFn: optOutFl, onSuccess: invalidateConsent })
  const isBusy = optInMutation.isPending || optOutMutation.isPending

  const optedIn = consentQuery.data?.opted_in ?? false
  // The demo endpoints only exist under the dev profile, so the link only shows where they answer.
  const demoQuery = useQuery({ queryKey: ['fl-demo-status'], queryFn: getFlDemoStatus, retry: false })

  return (
    <div className="space-y-6">
      <PageHeader
        title="Privacy & Federated Learning"
        description={
          <>
            Choose whether to contribute to the shared model. Participation is your decision, off by default, and reversible at any time. Read the{' '}
            <Link to="/privacy-policy" className="font-medium text-primary underline-offset-4 hover:underline">
              full privacy policy
            </Link>
            .
          </>
        }
        actions={
          <>
            {demoQuery.isSuccess && (
              <Button variant="outline" asChild>
                <Link to="/privacy/federated-learning">Federated learning demo</Link>
              </Button>
            )}
            <PageGuide guideId="privacy" />
          </>
        }
      />

      <section aria-label="In plain language" className="grid gap-3 md:grid-cols-3">
        {PLAIN_LANGUAGE_POINTS.map((point) => (
          <div key={point.title} className="surface-raised space-y-2 rounded-xl border p-4 sm:p-5">
            <span aria-hidden className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <point.icon className="size-4" />
            </span>
            <h3 className="font-heading text-sm font-semibold tracking-tight">{point.title}</h3>
            <p className="text-sm text-pretty text-muted-foreground">{point.body}</p>
          </div>
        ))}
      </section>

      <div data-guide="participation">
      <SectionCard
        title="Federated learning participation"
        action={!consentQuery.isLoading && <ToneBadge tone={optedIn ? 'success' : 'neutral'} dot>{optedIn ? 'Opted in' : 'Opted out'}</ToneBadge>}
      >
        {consentQuery.isLoading ? (
          <Skeleton className="h-9 w-40" />
        ) : (
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="max-w-2xl text-sm text-pretty text-muted-foreground">
              {optedIn
                ? 'Your sanitised feature vector is included in the next federated round. You can withdraw at any time. Withdrawal takes effect from the next round; anything already aggregated cannot be un-sent.'
                : 'You are not contributing to the shared model. Nothing is ever exported while opted out.'}
            </p>
            {optedIn ? (
              <Button variant="outline" className="shrink-0" disabled={isBusy} onClick={() => optOutMutation.mutate()}>
                <ShieldOffIcon aria-hidden />
                Withdraw from federation
              </Button>
            ) : (
              <Button className="shrink-0" disabled={isBusy} onClick={() => optInMutation.mutate()}>
                <ShieldCheckIcon aria-hidden />
                Opt in to federated learning
              </Button>
            )}
          </div>
        )}
      </SectionCard>
      </div>

      <div data-guide="schema">
      <SectionCard
        title="What is sent, exactly"
        description={schemaQuery.data ? `Exactly ${schemaQuery.data.feature_count} bounded numbers, derived only from your own activity. No raw counts, no free text, no identifiers.` : undefined}
      >
        {schemaQuery.isLoading && <LoadingRows rows={4} label="Loading the feature schema" />}
        {schemaQuery.isError && <ErrorState title="Couldn't load the feature schema" error={schemaQuery.error} onRetry={() => schemaQuery.refetch()} />}
        {schemaQuery.data && (
          <div className="space-y-5">
            <ul className="grid grid-cols-1 gap-2 text-xs sm:grid-cols-2 xl:grid-cols-3">
              {schemaQuery.data.features.map((feature) => (
                <li key={feature.name} className="flex items-center justify-between gap-2 rounded-lg border bg-surface-2/50 px-3 py-2">
                  <span className="truncate font-mono text-foreground">{feature.name}</span>
                  <span className="shrink-0 font-mono text-muted-foreground tabular-nums">
                    [{feature.lower_bound}, {feature.upper_bound}]
                  </span>
                </li>
              ))}
            </ul>
            <div className="space-y-2">
              <p className="eyebrow">What is never sent</p>
              <ul className="flex flex-wrap gap-1.5">
                {schemaQuery.data.not_collected.map((item) => (
                  <li key={item}>
                    <ToneBadge tone="danger">{item}</ToneBadge>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </SectionCard>
      </div>

      <div data-guide="ledger">
      <SectionCard
        title="Privacy Ledger"
        description="Everything this account has sent, one row per round. Expand a row to see the vector."
      >
        {ledgerQuery.isLoading && <LoadingRows rows={3} label="Loading the Privacy Ledger" />}
        {ledgerQuery.isError && <ErrorState title="Couldn't load the Privacy Ledger" error={ledgerQuery.error} onRetry={() => ledgerQuery.refetch()} />}
        {ledgerQuery.data && ledgerQuery.data.length === 0 && (
          <EmptyState
            compact
            icon={ScrollTextIcon}
            title="Nothing has been sent yet."
            description={optedIn ? 'Your first contribution appears here after the next federated round.' : 'Opt in above and each round you take part in is logged here.'}
          />
        )}
        {ledgerQuery.data && ledgerQuery.data.length > 0 && (
          <PrivacyLedgerTable entries={ledgerQuery.data} featureNames={schemaQuery.data?.features.map((feature) => feature.name)} />
        )}
      </SectionCard>
      </div>
    </div>
  )
}
