import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertTriangleIcon, PencilIcon, PlusIcon, Trash2Icon } from 'lucide-react'
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
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { formatLabel } from '@/lib/labels'
import { EmptyState } from '@/components/shared/EmptyState'

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

  const [deleteOpen, setDeleteOpen] = useState(false)
  const [questionText, setQuestionText] = useState('')
  const [category, setCategory] = useState<QuestionCategory>('technical')
  const [difficulty, setDifficulty] = useState<QuestionDifficulty | ''>('')

  const outcomeMutation = useMutation({
    mutationFn: (outcome: InterviewOutcome) => updateInterview(id!, { outcome }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['interview', id] }),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteInterview(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] })
      navigate('/interviews')
    },
  })

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
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (isError || !data) {
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

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">
            {data.company.name} — Round {data.round_number} ({formatLabel(data.round_type)})
          </h2>
          <p className="text-sm text-muted-foreground">
            <Link to={`/applications/${data.application_id}`} className="underline-offset-4 hover:underline">
              {data.job_title}
            </Link>
          </p>
        </div>
        <div className="flex items-center gap-2">
          {data.has_feedback ? (
            <Button variant="outline" size="sm" asChild>
              <Link to={`/interviews/${data.id}/debrief`}>View debrief</Link>
            </Button>
          ) : (
            <Button size="sm" asChild>
              <Link to={`/interviews/${data.id}/debrief`}>Start debrief</Link>
            </Button>
          )}
          <Button variant="outline" size="sm" asChild>
            <Link to={`/interviews/${data.id}/edit`}>
              <PencilIcon className="h-4 w-4" />
              Edit
            </Link>
          </Button>
          <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" size="sm">
                <Trash2Icon className="h-4 w-4" />
                Delete
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Delete this interview?</DialogTitle>
                <DialogDescription>
                  This permanently deletes round {data.round_number} and every question or debrief recorded against it.
                  This cannot be undone.
                </DialogDescription>
              </DialogHeader>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDeleteOpen(false)}>
                  Cancel
                </Button>
                <Button variant="destructive" onClick={() => deleteMutation.mutate()} disabled={deleteMutation.isPending}>
                  {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 text-sm sm:grid-cols-2">
            <Detail
              label="Scheduled"
              value={data.scheduled_at ? format(new Date(data.scheduled_at), 'MMM d, yyyy p') : 'Not scheduled'}
            />
            <Detail label="Mode" value={formatLabel(data.mode)} />
            <Detail label="Duration" value={data.duration_minutes ? `${data.duration_minutes} min` : '—'} />
            <Detail label="Interviewer" value={data.interviewer_name ?? '—'} />
            {data.notes && (
              <div className="sm:col-span-2">
                <p className="text-muted-foreground">Notes</p>
                <p className="whitespace-pre-wrap">{data.notes}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Outcome</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Select value={data.outcome} onValueChange={(value) => outcomeMutation.mutate(value as InterviewOutcome)}>
              <SelectTrigger>
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
            <Badge variant="outline">{data.has_feedback ? 'Debrief recorded' : 'No debrief yet'}</Badge>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Questions asked</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleAddQuestion} className="space-y-3 rounded-md border p-3">
            <Textarea
              placeholder="What were you asked? (added inline, no need to leave this page)"
              value={questionText}
              onChange={(event) => setQuestionText(event.target.value)}
              rows={2}
            />
            <div className="flex flex-wrap items-center gap-2">
              <Select value={category} onValueChange={(value) => setCategory(value as QuestionCategory)}>
                <SelectTrigger className="w-[160px]">
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
                <SelectTrigger className="w-[140px]">
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
              <Button type="submit" size="sm" disabled={!questionText.trim() || addQuestionMutation.isPending}>
                <PlusIcon className="h-4 w-4" />
                Add question
              </Button>
            </div>
          </form>

          {questionsQuery.data && questionsQuery.data.length > 0 ? (
            <ul className="space-y-2">
              {questionsQuery.data.map((question) => (
                <li key={question.id} className="flex items-start justify-between gap-3 rounded-md border p-3 text-sm">
                  <div>
                    <p>{question.question_text}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {formatLabel(question.category)}
                      {question.difficulty ? ` · ${formatLabel(question.difficulty)}` : ''}
                      {question.self_rating ? ` · self-rating ${question.self_rating}/5` : ''}
                    </p>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => deleteQuestionMutation.mutate(question.id)}
                    aria-label="Delete question"
                  >
                    <Trash2Icon className="h-4 w-4" />
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-muted-foreground">No questions recorded yet.</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-muted-foreground">{label}</p>
      <p>{value}</p>
    </div>
  )
}
