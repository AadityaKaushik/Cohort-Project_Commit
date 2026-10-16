import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { useUndoableDelete } from '@/hooks/use-undo-delete'
import { ClipboardListIcon, NotebookPenIcon, PencilIcon, PlusIcon, SearchXIcon, Trash2Icon } from 'lucide-react'
import {
  addInterviewQuestion,
  deleteInterview,
  deleteInterviewQuestion,
  getInterview,
  listInterviewQuestions,
  type InterviewOutcome,
  type QuestionCategory,
  type QuestionDifficulty,
  updateInterview,
} from '@/lib/interviews-api'
import { Button } from '@/components/ui/button'
import { CalendarDownloadButton } from '@/components/shared/CalendarDownloadButton'
import { downloadInterviewCalendar } from '@/lib/calendar-api'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { formatLabel } from '@/lib/labels'
import { listStories, storyKeys } from '@/lib/stories-api'
import { EmptyState } from '@/components/shared/EmptyState'
import { DetailList, type DetailItem } from '@/components/shared/DetailList'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { SaveToBankControl } from '@/components/interviews/SaveToBankControl'
import { OutcomeBadge, ToneBadge } from '@/components/shared/ToneBadge'

const OUTCOMES: InterviewOutcome[] = ['pending', 'passed', 'failed', 'unknown']
const CATEGORIES: QuestionCategory[] = ['dsa', 'technical', 'behavioural', 'hr', 'system_design', 'company_specific']
const DIFFICULTIES: QuestionDifficulty[] = ['easy', 'medium', 'hard']

export function InterviewDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['interview', id],
    queryFn: () => getInterview(id!),
    enabled: Boolean(id),
  })

  const questionsQuery = useQuery({
    queryKey: ['interview-questions', id],
    queryFn: () => listInterviewQuestions(id!),
    enabled: Boolean(id),
  })

  const storiesQuery = useQuery({ queryKey: storyKeys.all, queryFn: () => listStories(), enabled: Boolean(id) })
  const linkedStories = storiesQuery.data?.filter((story) => story.interview_id === id) ?? []

  const [deleteOpen, setDeleteOpen] = useState(false)
  const [questionText, setQuestionText] = useState('')
  const [category, setCategory] = useState<QuestionCategory>('technical')
  const [difficulty, setDifficulty] = useState<QuestionDifficulty | ''>('')

  const outcomeMutation = useMutation({
    mutationFn: (outcome: InterviewOutcome) => updateInterview(id!, { outcome }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['interview', id] }),
  })

  const undoableDelete = useUndoableDelete()

  function handleDelete() {
    setDeleteOpen(false)
    undoableDelete({
      id: id!,
      title: 'Interview deleted',
      send: () => deleteInterview(id!),
      onSettled: () => queryClient.invalidateQueries({ queryKey: ['interviews'] }),
      errorTitle: 'Could not delete the interview',
    })
    navigate('/interviews')
  }

  const addQuestionMutation = useMutation({
    mutationFn: () => addInterviewQuestion(id!, { question_text: questionText, category, difficulty: difficulty || null }),
    onSuccess: () => {
      setQuestionText('')
      setDifficulty('')
      queryClient.invalidateQueries({ queryKey: ['interview-questions', id] })
    },
  })

  const deleteQuestionMutation = useMutation({
    mutationFn: (questionId: string) => deleteInterviewQuestion(questionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['interview-questions', id] }),
  })

  function handleAddQuestion(event: FormEvent) {
    event.preventDefault()
    if (!questionText.trim()) return
    addQuestionMutation.mutate()
  }

  if (isLoading) {
    return <PageSkeleton label="Loading interview" />
  }

  if (isError || !data) {
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

  const details: DetailItem[] = [
    { label: 'Scheduled', value: data.scheduled_at ? format(new Date(data.scheduled_at), 'EEE d MMM yyyy, p') : 'Not scheduled' },
    { label: 'Mode', value: formatLabel(data.mode) },
    { label: 'Duration', value: data.duration_minutes ? `${data.duration_minutes} min` : '—' },
    { label: 'Interviewer', value: data.interviewer_name ?? '—' },
  ]
  if (data.notes) details.push({ label: 'Notes', value: data.notes, wide: true })

  return (
    <div className="space-y-6">
      <PageHeader
        back={{ to: '/interviews', label: 'Interviews' }}
        title={`${data.company.name} · Round ${data.round_number}`}
        description={
          <>
            {formatLabel(data.round_type)} for{' '}
            <Link to={`/applications/${data.application_id}`} className="font-medium text-foreground underline-offset-4 hover:underline">
              {data.job_title}
            </Link>
          </>
        }
        meta={<OutcomeBadge outcome={data.outcome} />}
        actions={
          <>
            {data.has_feedback ? (
              <Button variant="outline" asChild>
                <Link to={`/interviews/${data.id}/debrief`}>
                  <NotebookPenIcon aria-hidden />
                  View debrief
                </Link>
              </Button>
            ) : (
              <Button asChild>
                <Link to={`/interviews/${data.id}/debrief`}>
                  <NotebookPenIcon aria-hidden />
                  Start debrief
                </Link>
              </Button>
            )}
            <Button variant="outline" asChild>
              <Link to={`/interviews/${data.id}/prep`}>
                <ClipboardListIcon aria-hidden />
                Prep pack
              </Link>
            </Button>
            {data.scheduled_at && (
              <CalendarDownloadButton label="Add to calendar" download={() => downloadInterviewCalendar(data.id)} />
            )}
            <Button variant="outline" asChild>
              <Link to={`/interviews/${data.id}/edit`}>
                <PencilIcon aria-hidden />
                Edit
              </Link>
            </Button>
            <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
              <DialogTrigger asChild>
                <Button variant="destructive">
                  <Trash2Icon aria-hidden />
                  Delete
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Delete this interview?</DialogTitle>
                  <DialogDescription>
                    This permanently deletes round {data.round_number} and every question or debrief recorded against it. This cannot be
                    undone.
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setDeleteOpen(false)}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={handleDelete}>
                    Delete
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        }
      />

      <div className="stagger grid gap-6 lg:grid-cols-3">
        <SectionCard title="Details" className="lg:col-span-2">
          <DetailList items={details} />
        </SectionCard>

        <SectionCard title="Outcome" description={data.has_feedback ? 'Debrief recorded.' : 'No debrief yet.'}>
          <Select value={data.outcome} onValueChange={(value) => outcomeMutation.mutate(value as InterviewOutcome)}>
            <SelectTrigger aria-label="Outcome" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {OUTCOMES.map((outcome) => (
                <SelectItem key={outcome} value={outcome}>
                  {formatLabel(outcome)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </SectionCard>
      </div>

      <SectionCard title="Stories" description="STAR stories linked to this round.">
        <div className="space-y-3">
          {linkedStories.length > 0 ? (
            <ul className="divide-y rounded-lg border">
              {linkedStories.map((story) => (
                <li key={story.id} className="p-3 text-sm">
                  <Link to={`/stories/${story.id}`} className="font-medium underline-offset-4 hover:underline">
                    {story.title}
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-center text-sm text-muted-foreground">No stories linked yet.</p>
          )}
          <Button variant="outline" size="sm" asChild>
            <Link to={`/stories/new?interview_id=${data.id}`}>
              <PlusIcon aria-hidden />
              Add a story
            </Link>
          </Button>
        </div>
      </SectionCard>

      <SectionCard title="Questions asked" description="Log them inline; they feed your prep recommendations.">
        <div className="space-y-4">
          <form onSubmit={handleAddQuestion} className="space-y-3 rounded-lg border bg-surface-2/50 p-3">
            <Textarea
              aria-label="Question"
              placeholder="What were you asked?"
              value={questionText}
              onChange={(event) => setQuestionText(event.target.value)}
              rows={2}
              className="bg-card"
            />
            <div className="flex flex-wrap items-center gap-2">
              <Select value={category} onValueChange={(value) => setCategory(value as QuestionCategory)}>
                <SelectTrigger aria-label="Category" className="w-[160px] bg-card">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => (
                    <SelectItem key={c} value={c}>
                      {formatLabel(c)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={difficulty} onValueChange={(value) => setDifficulty(value as QuestionDifficulty)}>
                <SelectTrigger aria-label="Difficulty" className="w-[140px] bg-card">
                  <SelectValue placeholder="Difficulty" />
                </SelectTrigger>
                <SelectContent>
                  {DIFFICULTIES.map((d) => (
                    <SelectItem key={d} value={d}>
                      {formatLabel(d)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button type="submit" size="sm" className="ml-auto" disabled={!questionText.trim() || addQuestionMutation.isPending}>
                <PlusIcon aria-hidden />
                Add question
              </Button>
            </div>
          </form>

          {questionsQuery.data && questionsQuery.data.length > 0 ? (
            <ul className="divide-y rounded-lg border">
              {questionsQuery.data.map((question) => (
                <li key={question.id} className="flex items-start justify-between gap-3 p-3 text-sm transition-colors hover:bg-accent/40">
                  <div className="min-w-0">
                    <p>{question.question_text}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      <ToneBadge tone="neutral">{formatLabel(question.category)}</ToneBadge>
                      {question.difficulty && <ToneBadge tone="info">{formatLabel(question.difficulty)}</ToneBadge>}
                      {question.self_rating && <ToneBadge tone="primary">Self-rating {question.self_rating}/5</ToneBadge>}
                    </div>
                    <div className="mt-2">
                      <SaveToBankControl question={question} interviewId={data.id} />
                    </div>
                  </div>
                  <Button variant="ghost" size="icon-sm" onClick={() => deleteQuestionMutation.mutate(question.id)} aria-label="Delete question">
                    <Trash2Icon aria-hidden />
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-center text-sm text-muted-foreground">No questions recorded yet.</p>
          )}
        </div>
      </SectionCard>
    </div>
  )
}
