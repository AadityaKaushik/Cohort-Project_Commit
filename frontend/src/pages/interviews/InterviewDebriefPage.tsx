import { useEffect, useState } from 'react'
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
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { formatLabel } from '@/lib/labels'

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

  useEffect(() => {
    if (feedbackQuery.data) {
      setSelfRating(feedbackQuery.data.self_rating)
      setWhatWentWell(feedbackQuery.data.what_went_well ?? '')
      setWhatWentPoorly(feedbackQuery.data.what_went_poorly ?? '')
      setImprovementAreas(feedbackQuery.data.improvement_areas ?? '')
    }
  }, [feedbackQuery.data])

  const saveMutation = useMutation({
    mutationFn: (isDraft: boolean) =>
      upsertInterviewFeedback(id!, {
        self_rating: selfRating,
        what_went_well: whatWentWell || null,
        what_went_poorly: whatWentPoorly || null,
        improvement_areas: improvementAreas || null,
        is_draft: isDraft,
      }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['interview-feedback', id] })
      queryClient.invalidateQueries({ queryKey: ['interview', id] })
      if (!result.is_draft) {
        setJustSubmitted(true)
      }
    },
  })

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
      <PageHeader
        back={{ to: `/interviews/${interview.id}`, label: 'Interview' }}
        title="Post-interview debrief"
        description={`${interview.company.name} · Round ${interview.round_number} (${formatLabel(interview.round_type)})`}
      />

      {feedbackQuery.data?.is_draft && (
        <Alert>
          <NotebookPenIcon className="h-4 w-4" />
          <AlertTitle>Draft in progress</AlertTitle>
          <AlertDescription>You started this debrief earlier — pick up where you left off.</AlertDescription>
        </Alert>
      )}

      <SectionCard title="How do you think it went overall?" description="Every field is optional. A few words now beat a perfect write-up later.">
        <div className="space-y-5">
          <StarRating value={selfRating} onChange={setSelfRating} label="Overall self-rating" />
          <FormField id="whatWentWell" label="What went well?">
            <Textarea id="whatWentWell" value={whatWentWell} onChange={(event) => setWhatWentWell(event.target.value)} rows={2} placeholder="A quick note is enough" />
          </FormField>
          <FormField id="whatWentPoorly" label="What went poorly?">
            <Textarea id="whatWentPoorly" value={whatWentPoorly} onChange={(event) => setWhatWentPoorly(event.target.value)} rows={2} placeholder="A quick note is enough" />
          </FormField>
          <FormField id="improvementAreas" label="What would you improve next time?">
            <Textarea
              id="improvementAreas"
              value={improvementAreas}
              onChange={(event) => setImprovementAreas(event.target.value)}
              rows={2}
              placeholder="A quick note is enough"
            />
          </FormField>
          <FormActions>
            <Button variant="outline" onClick={() => saveMutation.mutate(true)} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Save draft'}
            </Button>
            <Button onClick={() => saveMutation.mutate(false)} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Submit debrief'}
            </Button>
          </FormActions>
        </div>
      </SectionCard>
    </div>
  )
}
