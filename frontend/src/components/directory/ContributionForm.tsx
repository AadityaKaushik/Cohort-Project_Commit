import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { FormActions, FormError, FormField } from '@/components/shared/Form'
import {
  CONTRIBUTION_OUTCOMES,
  CONTRIBUTION_ROLE_FAMILIES,
  ROUND_TYPES,
  createContribution,
  directoryKeys,
} from '@/lib/directory-api'
import { formatLabel } from '@/lib/labels'
import { textHints, textProblem } from '@/lib/text-quality'

export const CONSENT_LINE =
  'Sharing is optional. Your answers are combined with other candidates and shown only as group figures once 5 or more have shared. They are never shown one by one, and you can withdraw yours at any time.'

const SCALE = [1, 2, 3, 4, 5]

interface Props {
  companySlug?: string
  companyName?: string
  onDone?: () => void
}

/** US-96: structured, anonymous experience form. The company is fixed when opened from a profile. */
export function ContributionForm({ companySlug, companyName = '', onDone }: Props) {
  const queryClient = useQueryClient()
  const [name, setName] = useState(companyName)
  const [roleFamily, setRoleFamily] = useState<string>(CONTRIBUTION_ROLE_FAMILIES[0])
  const [level, setLevel] = useState<'fresher' | 'experienced'>('fresher')
  const [year, setYear] = useState(new Date().getFullYear())
  const [rounds, setRounds] = useState<Record<string, number>>({})
  const [overallDifficulty, setOverallDifficulty] = useState(3)
  const [overallExperience, setOverallExperience] = useState(3)
  const [outcome, setOutcome] = useState<string>('pending')
  const [tip, setTip] = useState('')
  const [consent, setConsent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const mutation = useMutation({
    mutationFn: createContribution,
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: directoryKeys.detail(saved.company_slug) })
      queryClient.invalidateQueries({ queryKey: directoryKeys.mine })
      setDone(true)
      onDone?.()
    },
    onError: (failure: Error) => setError(failure.message),
  })

  function toggleRound(roundType: string) {
    setRounds((current) => {
      const next = { ...current }
      if (roundType in next) {
        delete next[roundType]
      } else {
        next[roundType] = 3
      }
      return next
    })
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (!companySlug && name.trim() === '') {
      setError('Enter the company name.')
      return
    }
    if (Object.keys(rounds).length === 0) {
      setError('Pick at least one round you went through.')
      return
    }
    const trimmedTip = tip.trim()
    if (trimmedTip !== '') {
      const problem = textProblem(trimmedTip)
      if (problem) {
        setError(problem.message)
        return
      }
      if (textHints(trimmedTip, true).some((hint) => hint.code === 'CONTAINS_CONTACT_DETAILS')) {
        setError('Remove email addresses and phone numbers. The tip is shown to other candidates.')
        return
      }
    }
    mutation.mutate({
      ...(companySlug ? { company_slug: companySlug } : { company_name: name.trim() }),
      role_family: roleFamily,
      level,
      year,
      rounds: Object.entries(rounds).map(([round_type, difficulty]) => ({ round_type, difficulty })),
      overall_difficulty: overallDifficulty,
      overall_experience: overallExperience,
      outcome,
      tip: trimmedTip === '' ? null : trimmedTip,
    })
  }

  if (done) {
    return <p role="status" className="text-sm">Thanks. Your answers were saved and will count towards the group figures.</p>
  }

  return (
    <form onSubmit={submit} className="space-y-4" aria-label="Share your experience">
      <p role="note" className="rounded-lg border bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
        {CONSENT_LINE}
      </p>

      {!companySlug && (
        <FormField id="contrib-company" label="Company" hint="A new company is added to the directory with no sourced pattern.">
          <Input value={name} onChange={(event) => setName(event.target.value)} maxLength={200} />
        </FormField>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <FormField id="contrib-role" label="Role family">
          <NativeSelect value={roleFamily} onChange={(event) => setRoleFamily(event.target.value)}>
            {CONTRIBUTION_ROLE_FAMILIES.map((family) => (
              <option key={family} value={family}>
                {formatLabel(family)}
              </option>
            ))}
          </NativeSelect>
        </FormField>
        <FormField id="contrib-level" label="Level">
          <NativeSelect value={level} onChange={(event) => setLevel(event.target.value as 'fresher' | 'experienced')}>
            <option value="fresher">Fresher</option>
            <option value="experienced">Experienced</option>
          </NativeSelect>
        </FormField>
        <FormField id="contrib-year" label="Year">
          <Input type="number" value={year} min={2000} max={new Date().getFullYear()} onChange={(event) => setYear(Number(event.target.value))} />
        </FormField>
      </div>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">Rounds you went through</legend>
        <ul className="space-y-2">
          {ROUND_TYPES.map((roundType) => {
            const checked = roundType in rounds
            return (
              <li key={roundType} className="flex flex-wrap items-center gap-3 text-sm">
                <label className="flex min-w-48 items-center gap-2">
                  <input type="checkbox" checked={checked} onChange={() => toggleRound(roundType)} />
                  {formatLabel(roundType)}
                </label>
                {checked && (
                  <label className="flex items-center gap-2 text-muted-foreground">
                    Difficulty
                    <NativeSelect
                      aria-label={`${formatLabel(roundType)} difficulty`}
                      className="w-20"
                      value={rounds[roundType]}
                      onChange={(event) => setRounds({ ...rounds, [roundType]: Number(event.target.value) })}
                    >
                      {SCALE.map((value) => (
                        <option key={value} value={value}>
                          {value}
                        </option>
                      ))}
                    </NativeSelect>
                  </label>
                )}
              </li>
            )
          })}
        </ul>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-3">
        <FormField id="contrib-difficulty" label="Overall difficulty (1 easy, 5 hard)">
          <NativeSelect value={overallDifficulty} onChange={(event) => setOverallDifficulty(Number(event.target.value))}>
            {SCALE.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </NativeSelect>
        </FormField>
        <FormField id="contrib-experience" label="Overall experience (1 poor, 5 good)">
          <NativeSelect value={overallExperience} onChange={(event) => setOverallExperience(Number(event.target.value))}>
            {SCALE.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </NativeSelect>
        </FormField>
        <FormField id="contrib-outcome" label="Outcome">
          <NativeSelect value={outcome} onChange={(event) => setOutcome(event.target.value)}>
            {CONTRIBUTION_OUTCOMES.map((value) => (
              <option key={value} value={value}>
                {formatLabel(value)}
              </option>
            ))}
          </NativeSelect>
        </FormField>
      </div>

      <FormField id="contrib-tip" label="One tip for others (optional)" hint="Up to 280 characters. No names, emails or phone numbers.">
        <Textarea value={tip} maxLength={280} rows={2} onChange={(event) => setTip(event.target.value)} />
      </FormField>

      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" className="mt-1" checked={consent} onChange={(event) => setConsent(event.target.checked)} />I agree to share
        these answers anonymously as described above.
      </label>

      {error && <FormError>{error}</FormError>}
      <FormActions>
        <Button type="submit" disabled={!consent || mutation.isPending}>
          Share my experience
        </Button>
      </FormActions>
    </form>
  )
}
