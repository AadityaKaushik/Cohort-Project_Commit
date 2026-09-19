import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangleIcon, CheckCircle2Icon } from 'lucide-react'
import { getInterview, getInterviewFeedbackOrNull, upsertInterviewFeedback } from '@/lib/interviews-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { StarRating } from '@/components/shared/StarRating'

/**
 * The guided post-interview debrief (US-30). Deliberately a single screen
 * with three short prompts and a star rating — no multi-step wizard, no
 * required fields beyond what the caller can reliably produce from memory
 * (DR-07) — because that is what keeps it completable in under three
 * minutes (NFR-31): every extra screen or mandatory field is a minute a
 * candidate will not spend before the memory decays.
 */
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
    return (
      <div className="mx-auto max-w-xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (interviewQuery.isError || !interviewQuery.data) {
    return (
      <EmptyState
        icon={AlertTriangleIcon}
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
      <div className="mx-auto max-w-xl">
        <EmptyState
          icon={CheckCircle2Icon}
          title="Debrief saved"
          description="Your notes are captured while they're still fresh — you can always come back and edit them."
          action={
            <Button asChild>
              <Link to={`/interviews/${interview.id}`}>Back to interview</Link>
            </Button>
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div>
        <h2 className="text-lg font-semibold">Post-interview debrief</h2>
        <p className="text-sm text-muted-foreground">
          {interview.company.name} — Round {interview.round_number} ({interview.round_type.replace('_', ' ')})
        </p>
      </div>

      {feedbackQuery.data?.is_draft && (
        <Alert>
          <AlertTitle>Draft in progress</AlertTitle>
          <AlertDescription>You started this debrief earlier — pick up where you left off.</AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">How do you think it went overall?</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          <StarRating value={selfRating} onChange={setSelfRating} label="Overall self-rating" />

          <div className="space-y-2">
            <Label htmlFor="whatWentWell">What went well?</Label>
            <Textarea
              id="whatWentWell"
              value={whatWentWell}
              onChange={(event) => setWhatWentWell(event.target.value)}
              rows={2}
              placeholder="Optional — a quick note is enough"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="whatWentPoorly">What went poorly?</Label>
            <Textarea
              id="whatWentPoorly"
              value={whatWentPoorly}
              onChange={(event) => setWhatWentPoorly(event.target.value)}
              rows={2}
              placeholder="Optional — a quick note is enough"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="improvementAreas">What would you improve next time?</Label>
            <Textarea
              id="improvementAreas"
              value={improvementAreas}
              onChange={(event) => setImprovementAreas(event.target.value)}
              rows={2}
              placeholder="Optional — a quick note is enough"
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2">
            <Badge variant="outline">Every field above is optional — save whatever you remember</Badge>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => saveMutation.mutate(true)}
                disabled={saveMutation.isPending}
              >
                {saveMutation.isPending ? 'Saving…' : 'Save draft'}
              </Button>
              <Button onClick={() => saveMutation.mutate(false)} disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : 'Submit debrief'}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
