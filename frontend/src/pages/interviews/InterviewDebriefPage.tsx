import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2Icon, NotebookPenIcon, SearchXIcon } from 'lucide-react'
import { getInterview, getInterviewFeedbackOrNull, upsertInterviewFeedback } from '@/lib/interviews-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { StarRating } from '@/components/shared/StarRating'
import { FormActions, FormField } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { formatLabel } from '@/lib/labels'
import { textProblem } from '@/lib/text-quality'
import { useSoftWarnings } from '@/hooks/use-soft-warnings'
import { SoftWarning } from '@/components/shared/SoftWarning'
import { clearDraft, readDraft, writeDraft } from '@/lib/drafts'
import { useUnsavedChanges } from '@/components/shared/UnsavedChangesGuard'
import { OUTCOME_CHIPS, ROUND_TOPICS, readTopics, withTopics } from '@/lib/debrief-topics'
import { cn } from '@/lib/utils'

interface DebriefDraft {
  selfRating: number | null
  whatWentWell: string
  whatWentPoorly: string
  improvementAreas: string
}

export function InterviewDebriefPage() {
  const { id } = useParams()
  const queryClient = useQueryClient()

  const interviewQuery = useQuery({
    queryKey: ['interview', id],
    queryFn: () => getInterview(id!),
    enabled: Boolean(id),
  })

  const feedbackQuery = useQuery({
    queryKey: ['interview-feedback', id],
    queryFn: () => getInterviewFeedbackOrNull(id!),
    enabled: Boolean(id),
  })

  const [selfRating, setSelfRating] = useState<number | null>(null)
  const [whatWentWell, setWhatWentWell] = useState('')
  const [whatWentPoorly, setWhatWentPoorly] = useState('')
  const [improvementAreas, setImprovementAreas] = useState('')
  const [justSubmitted, setJustSubmitted] = useState(false)
  const [chosenTopics, setChosenTopics] = useState<string[] | null>(null)
  const [outcomeChosen, setOutcomeChosen] = useState(false)

  const draftKey = `debrief-${id}`
  const [saved, setSaved] = useState<DebriefDraft>({ selfRating: null, whatWentWell: '', whatWentPoorly: '', improvementAreas: '' })
  const current = useMemo<DebriefDraft>(
    () => ({ selfRating, whatWentWell, whatWentPoorly, improvementAreas }),
    [selfRating, whatWentWell, whatWentPoorly, improvementAreas],
  )
  const dirty = JSON.stringify(current) !== JSON.stringify(saved)
  const { guard, markSaved } = useUnsavedChanges(dirty && !justSubmitted)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const softWarnings = useSoftWarnings()
  const lastActionIsDraft = useRef(false)

  useEffect(() => {
    if (feedbackQuery.isLoading) {
      return
    }
    const data = feedbackQuery.data
    const fromServer: DebriefDraft = {
      selfRating: data?.self_rating ?? null,
      whatWentWell: data?.what_went_well ?? '',
      whatWentPoorly: data?.what_went_poorly ?? '',
      improvementAreas: data?.improvement_areas ?? '',
    }
    setSaved(fromServer)
    const draft = readDraft<DebriefDraft>(draftKey) ?? fromServer
    setSelfRating(draft.selfRating)
    setWhatWentWell(draft.whatWentWell)
    setWhatWentPoorly(draft.whatWentPoorly)
    setImprovementAreas(draft.improvementAreas)
    setChosenTopics(readTopics(draft.improvementAreas))
  }, [feedbackQuery.data, feedbackQuery.isLoading, draftKey])

  useEffect(() => {
    if (dirty) {
      writeDraft(draftKey, current)
    }
  }, [dirty, current, draftKey])

  const roundTopics = interviewQuery.data ? ROUND_TOPICS[interviewQuery.data.round_type] ?? [] : []
  const topics = chosenTopics ?? roundTopics

  function toggleTopic(topic: string) {
    setChosenTopics(topics.includes(topic) ? topics.filter((item) => item !== topic) : [...topics, topic])
  }

  const saveMutation = useMutation({
    mutationFn: (isDraft: boolean) =>
      upsertInterviewFeedback(id!, {
        self_rating: selfRating,
        what_went_well: whatWentWell || null,
        what_went_poorly: whatWentPoorly || null,
        improvement_areas: (outcomeChosen ? withTopics(improvementAreas, topics) : improvementAreas) || null,
        is_draft: isDraft,
      }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['interview-feedback', id] })
      queryClient.invalidateQueries({ queryKey: ['interview', id] })
      clearDraft(draftKey)
      setSaved(current)
      if (!result.is_draft) {
        markSaved()
        setJustSubmitted(true)
      }
    },
  })

  const textFields = [
    { id: 'whatWentWell', value: whatWentWell },
    { id: 'whatWentPoorly', value: whatWentPoorly },
    { id: 'improvementAreas', value: improvementAreas },
  ]

  function save(isDraft: boolean) {
    lastActionIsDraft.current = isDraft
    const problems: Record<string, string> = {}
    for (const field of textFields) {
      const problem = field.value.trim() === '' ? null : textProblem(field.value)
      if (problem) {
        problems[field.id] = problem.message
      }
    }
    setFieldErrors(problems)
    if (Object.keys(problems).length > 0) {
      return
    }
    if (!isDraft) {
      const warned = textFields.filter((field) => field.value.trim() !== '' && softWarnings.check(field.id, field.value, true).length > 0)
      if (warned.length > 0) {
        return
      }
    }
    saveMutation.mutate(isDraft)
  }

  function warningFor(field: { id: string; value: string }) {
    return (
      <SoftWarning
        id={field.id}
        messages={softWarnings.shown[field.id]}
        onEdit={() => {
          softWarnings.dismiss(field.id)
          document.getElementById(field.id)?.focus()
        }}
        onSaveAnyway={() => {
          softWarnings.accept(field.id, field.value)
          save(lastActionIsDraft.current)
        }}
      />
    )
  }

  if (interviewQuery.isLoading || feedbackQuery.isLoading) {
    return <PageSkeleton label="Loading debrief" variant="form" />
  }

  if (interviewQuery.isError || !interviewQuery.data) {
    return (
      <EmptyState
        icon={SearchXIcon}
        title="Interview not found"
        description="It may have been deleted, or the link may be wrong."
        action={
          <Button asChild>
            <Link to="/interviews">Back to interviews</Link>
          </Button>
        }
      />
    )
  }

  const interview = interviewQuery.data

  if (justSubmitted) {
    return (
      <div className="mx-auto max-w-2xl">
        <EmptyState
          icon={CheckCircle2Icon}
          title="Debrief saved"
          description="Your notes are captured while they're still fresh — you can always come back and edit them."
          quote={2}
          action={
            <Button asChild>
              <Link to={`/interviews/${interview.id}`}>Back to interview</Link>
            </Button>
          }
          secondaryAction={
            <Button variant="outline" asChild>
              <Link to="/prep">Open prep plan</Link>
            </Button>
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {guard}
      <PageHeader
        back={{ to: `/interviews/${interview.id}`, label: 'Interview' }}
        title="Post-interview debrief"
        description={`${interview.company.name} · Round ${interview.round_number} (${formatLabel(interview.round_type)})`}
        actions={<PageGuide guideId="debrief" />}
      />

      {feedbackQuery.data?.is_draft && (
        <Alert>
          <NotebookPenIcon className="h-4 w-4" />
          <AlertTitle>Draft in progress</AlertTitle>
          <AlertDescription>You started this debrief earlier — pick up where you left off.</AlertDescription>
        </Alert>
      )}

      <SectionCard title="How did it go?" description="One tap is enough. The date, round and company are already filled in.">
        <div className="space-y-4">
          <div role="group" aria-label="Outcome" data-guide="outcome" className="flex flex-wrap gap-2">
            {OUTCOME_CHIPS.map((chip) => (
              <Chip
                key={chip.key}
                selected={outcomeChosen && selfRating === chip.rating}
                onClick={() => {
                  setSelfRating(chip.rating)
                  setOutcomeChosen(true)
                }}
              >
                {chip.label}
              </Chip>
            ))}
          </div>
          <div role="group" aria-label="Topics that came up" data-guide="topics" className="flex flex-wrap gap-2">
            {roundTopics
              .concat(topics.filter((topic) => !roundTopics.includes(topic)))
              .map((topic) => (
                <Chip key={topic} selected={topics.includes(topic)} onClick={() => toggleTopic(topic)}>
                  {topic}
                </Chip>
              ))}
          </div>
          {outcomeChosen && (
            <Button onClick={() => saveMutation.mutate(false)} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Save debrief'}
            </Button>
          )}
        </div>
      </SectionCard>

      <SectionCard title="Add more detail" description="All optional. A few words now beat a perfect write-up later.">
        <div data-guide="detail" className="space-y-5">
          <StarRating value={selfRating} onChange={setSelfRating} label="Overall self-rating" />
          <FormField id="whatWentWell" label="What went well?" error={fieldErrors.whatWentWell}>
            <Textarea
              id="whatWentWell"
              value={whatWentWell}
              onChange={(event) => setWhatWentWell(event.target.value)}
              onBlur={() => softWarnings.check('whatWentWell', whatWentWell, true)}
              rows={2}
              placeholder="A quick note is enough"
            />
          </FormField>
          {warningFor(textFields[0])}
          <FormField id="whatWentPoorly" label="What went poorly?" error={fieldErrors.whatWentPoorly}>
            <Textarea
              id="whatWentPoorly"
              value={whatWentPoorly}
              onChange={(event) => setWhatWentPoorly(event.target.value)}
              onBlur={() => softWarnings.check('whatWentPoorly', whatWentPoorly, true)}
              rows={2}
              placeholder="A quick note is enough"
            />
          </FormField>
          {warningFor(textFields[1])}
          <FormField id="improvementAreas" label="What would you improve next time?" error={fieldErrors.improvementAreas}>
            <Textarea
              id="improvementAreas"
              value={improvementAreas}
              onChange={(event) => setImprovementAreas(event.target.value)}
              onBlur={() => softWarnings.check('improvementAreas', improvementAreas, true)}
              rows={2}
              placeholder="A quick note is enough"
            />
          </FormField>
          {warningFor(textFields[2])}
          <FormActions>
            <Button variant="outline" onClick={() => save(true)} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Save draft'}
            </Button>
            <Button onClick={() => save(false)} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Submit debrief'}
            </Button>
          </FormActions>
        </div>
      </SectionCard>
    </div>
  )
}

function Chip({ selected, onClick, children }: { selected: boolean; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={cn(
        'rounded-full border px-3 py-1 text-sm transition-colors',
        selected ? 'border-primary bg-primary/10 text-primary' : 'bg-card text-muted-foreground hover:border-primary/40',
      )}
    >
      {children}
    </button>
  )
}
