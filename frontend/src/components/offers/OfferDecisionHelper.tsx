import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { NativeSelect } from '@/components/ui/native-select'
import {
  getOfferDecision,
  OFFER_FACTORS,
  offerDecisionKey,
  saveOfferRatings,
  saveOfferWeights,
  type FactorValues,
  type OfferDecision,
  type OfferFactor,
} from '@/lib/offers-api'

function WeightSlider({ factor, label, value, cacheKey }: { factor: OfferFactor; label: string; value: number; cacheKey: readonly string[] }) {
  const queryClient = useQueryClient()
  const [pending, setPending] = useState<number | null>(null)
  const save = useMutation({
    mutationFn: (weight: number) => saveOfferWeights({ [factor]: weight }),
    onSuccess: (data) => queryClient.setQueryData<OfferDecision>(cacheKey, data),
    onSettled: () => setPending(null),
  })
  const shown = pending ?? value

  return (
    <div className="flex items-center gap-2">
      <input
        type="range"
        min={1}
        max={5}
        step={1}
        className="w-24 accent-primary"
        aria-label={`Weight for ${label}`}
        value={shown}
        onChange={(event) => {
          const next = Number(event.target.value)
          setPending(next)
          save.mutate(next)
        }}
      />
      <span className="w-4 text-xs tabular-nums text-muted-foreground">{shown}</span>
    </div>
  )
}

function RatingSelect({ offerId, company, factor, label, value, cacheKey }: {
  cacheKey: readonly string[]
  offerId: string
  company: string
  factor: OfferFactor
  label: string
  value: number | undefined
}) {
  const queryClient = useQueryClient()
  const save = useMutation({
    mutationFn: (rating: number) => saveOfferRatings(offerId, { [factor]: rating } as FactorValues),
    onSuccess: (data) => queryClient.setQueryData<OfferDecision>(cacheKey, data),
  })

  return (
    <NativeSelect
      aria-label={`${label} rating for ${company}`}
      className="w-20"
      value={value ?? ''}
      onChange={(event) => event.target.value && save.mutate(Number(event.target.value))}
    >
      <option value="">Rate</option>
      {[1, 2, 3, 4, 5].map((rating) => (
        <option key={rating} value={rating}>
          {rating}
        </option>
      ))}
    </NativeSelect>
  )
}

export function OfferDecisionHelper({ offerIds }: { offerIds: string[] }) {
  const cacheKey = offerDecisionKey(offerIds)
  const query = useQuery({ queryKey: cacheKey, queryFn: () => getOfferDecision(offerIds) })
  const entries = query.data?.offers ?? []

  if (query.isLoading || !query.data) {
    return null
  }

  return (
    <section aria-label="Decision helper" className="space-y-3 rounded-lg border p-4">
      <div>
        <h3 className="text-sm font-semibold">Decision helper</h3>
        <p className="text-xs text-muted-foreground">
          Set how much each factor matters to you, then rate every offer from 1 to 5. The score is only a way to see your own priorities. It
          never changes an offer.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <caption className="sr-only">Weights and ratings per factor</caption>
          <thead>
            <tr className="border-b text-left">
              <th scope="col" className="py-2 pr-3 font-medium">Factor</th>
              <th scope="col" className="py-2 pr-3 font-medium">Weight (1 to 5)</th>
              {entries.map((entry) => (
                <th key={entry.offer_id} scope="col" className="py-2 pr-3 font-medium">
                  {entry.company_name}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {OFFER_FACTORS.map(({ key, label }) => (
              <tr key={key} className="border-b last:border-0">
                <th scope="row" className="py-2 pr-3 text-left font-normal text-muted-foreground">
                  {label}
                </th>
                <td className="py-2 pr-3">
                  <WeightSlider factor={key} label={label} value={query.data.weights[key] ?? 3} cacheKey={cacheKey} />
                </td>
                {entries.map((entry) => (
                  <td key={entry.offer_id} className="py-2 pr-3">
                    <RatingSelect
                      cacheKey={cacheKey}
                      offerId={entry.offer_id}
                      company={entry.company_name}
                      factor={key}
                      label={label}
                      value={entry.ratings[key]}
                    />
                  </td>
                ))}
              </tr>
            ))}
            <tr>
              <th scope="row" className="py-2 pr-3 text-left font-medium">Score</th>
              <td />
              {entries.map((entry) => (
                <td key={entry.offer_id} className="py-2 pr-3 font-semibold tabular-nums">
                  {entry.score === null ? <span className="text-xs font-normal text-muted-foreground">Rate all six</span> : `${entry.score} / 100`}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>

      {query.data.leader_summary ? (
        <p className="text-sm" data-testid="decision-summary">
          {query.data.leader_summary}
        </p>
      ) : (
        <p className="text-sm text-muted-foreground">Rate every factor on at least two offers to see which one leads and why.</p>
      )}
    </section>
  )
}
