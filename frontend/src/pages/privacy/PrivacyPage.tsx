import { ShieldCheckIcon, ShieldOffIcon } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getFlConsent, getFlSchema, optInFl, optOutFl } from '@/lib/fl-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * US-57/US-64: the opt-in consent screen. Shows the exact feature schema
 * (not a paraphrase of it) so "what is sent" is verifiable, not just
 * claimed, and a companion "what is never sent" list right next to it.
 * Default is off — {@code opted_in: false} until the user explicitly
 * flips this toggle (enforced at the DB level too, V1's
 * {@code fl_opt_in NOT NULL DEFAULT false}).
 */
export function PrivacyPage() {
  const queryClient = useQueryClient()

  const consentQuery = useQuery({ queryKey: ['fl-consent'], queryFn: getFlConsent })
  const schemaQuery = useQuery({ queryKey: ['fl-schema'], queryFn: getFlSchema })

  function invalidateConsent() {
    queryClient.invalidateQueries({ queryKey: ['fl-consent'] })
  }

  const optInMutation = useMutation({ mutationFn: optInFl, onSuccess: invalidateConsent })
  const optOutMutation = useMutation({ mutationFn: optOutFl, onSuccess: invalidateConsent })
  const isBusy = optInMutation.isPending || optOutMutation.isPending

  const optedIn = consentQuery.data?.opted_in ?? false

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Privacy &amp; Federated Learning</h2>
        <p className="text-sm text-muted-foreground">
          Choose whether to contribute to the shared model. Participation is your decision, off by default, and
          reversible at any time.
        </p>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
          <CardTitle className="text-base font-medium">Federated learning participation</CardTitle>
          {!consentQuery.isLoading && (
            <Badge variant={optedIn ? 'default' : 'outline'}>
              {optedIn ? 'Opted in' : 'Opted out'}
            </Badge>
          )}
        </CardHeader>
        <CardContent className="space-y-4">
          {consentQuery.isLoading && <Skeleton className="h-9 w-40" />}

          {!consentQuery.isLoading && (
            <>
              <p className="text-sm text-muted-foreground">
                {optedIn
                  ? 'Your sanitised feature vector is included in the next federated round. You can withdraw at any time — withdrawal takes effect from the next round; anything already aggregated cannot be un-sent.'
                  : 'You are not contributing to the shared model. Nothing is ever exported while opted out.'}
              </p>
              {optedIn ? (
                <Button variant="outline" disabled={isBusy} onClick={() => optOutMutation.mutate()}>
                  <ShieldOffIcon className="h-4 w-4" />
                  Withdraw from federation
                </Button>
              ) : (
                <Button disabled={isBusy} onClick={() => optInMutation.mutate()}>
                  <ShieldCheckIcon className="h-4 w-4" />
                  Opt in to federated learning
                </Button>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-medium">What is sent, exactly</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {schemaQuery.isLoading && (
            <div className="space-y-2">
              {Array.from({ length: 4 }).map((_, index) => (
                <Skeleton key={index} className="h-5 w-full" />
              ))}
            </div>
          )}

          {schemaQuery.isError && (
            <Alert variant="destructive">
              <AlertTitle>Couldn&apos;t load the feature schema</AlertTitle>
              <AlertDescription>Something went wrong talking to the server.</AlertDescription>
            </Alert>
          )}

          {schemaQuery.data && (
            <>
              <p className="text-sm text-muted-foreground">
                Exactly {schemaQuery.data.feature_count} bounded numbers, derived only from your own activity. No raw
                counts, no free text, no identifiers.
              </p>
              <ul className="grid grid-cols-1 gap-x-4 gap-y-1 text-xs text-muted-foreground sm:grid-cols-2">
                {schemaQuery.data.features.map((feature) => (
                  <li key={feature.name} className="flex justify-between gap-2 rounded border border-dashed px-2 py-1">
                    <span className="font-mono text-foreground">{feature.name}</span>
                    <span>
                      [{feature.lower_bound}, {feature.upper_bound}]
                    </span>
                  </li>
                ))}
              </ul>

              <div>
                <p className="text-sm font-medium">What is never sent</p>
                <ul className="mt-1 flex flex-wrap gap-1">
                  {schemaQuery.data.not_collected.map((item) => (
                    <Badge key={item} variant="outline">
                      {item}
                    </Badge>
                  ))}
                </ul>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
