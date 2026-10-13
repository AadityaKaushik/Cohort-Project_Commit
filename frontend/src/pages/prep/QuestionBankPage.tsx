import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDownIcon, ChevronRightIcon, ListChecksIcon } from 'lucide-react'
import { format } from 'date-fns'
import {
  getPreparationProgress,
  listPracticeAttempts,
  listQuestions,
  listTopics,
  recordPracticeAttempt,
  type Difficulty,
  type QuestionBankItem,
  type QuestionCategory,
} from '@/lib/prep-api'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { useToast } from '@/hooks/use-toast'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { LogAttemptDialog, type AttemptDraft } from '@/components/prep/LogAttemptDialog'
import { MasteryBadge } from '@/components/prep/MasteryBadge'

const CATEGORIES: QuestionCategory[] = ['dsa', 'technical', 'behavioural', 'hr', 'system_design', 'company_specific']
const DIFFICULTIES: Difficulty[] = ['easy', 'medium', 'hard']
const PAGE_SIZE = 20

export function QuestionBankPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [params, setParams] = useSearchParams()
  const [logging, setLogging] = useState<QuestionBankItem | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const topicId = params.get('topic_id') ?? ''
  const category = (params.get('category') ?? '') as QuestionCategory | ''
  const difficulty = (params.get('difficulty') ?? '') as Difficulty | ''
  const q = params.get('q') ?? ''
  const page = Number(params.get('page') ?? '0')

  function setParam(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) {
      next.set(key, value)
    } else {
      next.delete(key)
    }
    if (key !== 'page') {
      next.delete('page')
    }
    setParams(next)
  }

  const topicsQuery = useQuery({ queryKey: ['topics'], queryFn: listTopics })
  const progressQuery = useQuery({ queryKey: ['preparation-progress'], queryFn: getPreparationProgress })
  const questionsQuery = useQuery({
    queryKey: ['questions', topicId, category, difficulty, q, page],
    queryFn: () =>
      listQuestions({
        topic_id: topicId || undefined,
        category: category || undefined,
        difficulty: difficulty || undefined,
        q: q || undefined,
        page,
        size: PAGE_SIZE,
      }),
  })

  const attemptMutation = useMutation({
    mutationFn: ({ question, draft }: { question: QuestionBankItem; draft: AttemptDraft }) =>
      recordPracticeAttempt(question.id, draft),
    onSuccess: async (_attempt, { question }) => {
      setLogging(null)
      queryClient.invalidateQueries({ queryKey: ['attempts', question.id] })
      const progress = await queryClient.fetchQuery({ queryKey: ['preparation-progress'], queryFn: getPreparationProgress, staleTime: 0 })
      const topic = progress.topics.find((candidate) => candidate.topic_id === question.topic_id)
      const description = !topic
        ? undefined
        : topic.sufficient_evidence
          ? `${topic.topic_name} mastery is now ${Math.round(topic.mastery_score * 100)}%.`
          : `${topic.topic_name} needs one more attempt before mastery is shown.`
      toast({ title: 'Attempt logged', description })
    },
    onError: (error: unknown) => {
      toast({
        title: 'Could not log the attempt',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        variant: 'error',
      })
    },
  })

  const topicMastery = progressQuery.data?.topics.find((topic) => topic.topic_id === topicId)
  const selectedTopic = topicsQuery.data?.find((topic) => topic.id === topicId)
  const total = questionsQuery.data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold">{selectedTopic ? selectedTopic.name : 'Question bank'}</h2>
          <p className="text-sm text-muted-foreground">
            Log every attempt. Mastery for a topic moves as your confidence and recency change.
          </p>
        </div>
        {topicMastery && (
          <MasteryBadge
            masteryScore={topicMastery.mastery_score}
            sufficientEvidence={topicMastery.sufficient_evidence}
            observationCount={topicMastery.observation_count}
          />
        )}
      </div>

      <form role="search" aria-label="Filter questions" className="flex flex-wrap gap-2" onSubmit={(e) => e.preventDefault()}>
        <div className="min-w-48 flex-1">
          <label htmlFor="bank-q" className="sr-only">
            Search questions
          </label>
          <Input id="bank-q" type="search" placeholder="Search questions" value={q} onChange={(e) => setParam('q', e.target.value)} />
        </div>
        <div className="w-44">
          <label htmlFor="bank-topic" className="sr-only">
            Topic
          </label>
          <NativeSelect id="bank-topic" value={topicId} onChange={(e) => setParam('topic_id', e.target.value)}>
            <option value="">All topics</option>
            {topicsQuery.data?.map((topic) => (
              <option key={topic.id} value={topic.id}>
                {topic.name}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="w-40">
          <label htmlFor="bank-category" className="sr-only">
            Category
          </label>
          <NativeSelect id="bank-category" value={category} onChange={(e) => setParam('category', e.target.value)}>
            <option value="">All categories</option>
            {CATEGORIES.map((option) => (
              <option key={option} value={option}>
                {formatLabel(option)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="w-36">
          <label htmlFor="bank-difficulty" className="sr-only">
            Difficulty
          </label>
          <NativeSelect id="bank-difficulty" value={difficulty} onChange={(e) => setParam('difficulty', e.target.value)}>
            <option value="">Any difficulty</option>
            {DIFFICULTIES.map((option) => (
              <option key={option} value={option}>
                {formatLabel(option)}
              </option>
            ))}
          </NativeSelect>
        </div>
      </form>

      {questionsQuery.isLoading && <LoadingRows label="Loading questions" />}
      {questionsQuery.isError && <ErrorState title="Couldn't load questions" onRetry={() => questionsQuery.refetch()} />}
      {questionsQuery.data && questionsQuery.data.items.length === 0 && (
        <EmptyState
          icon={ListChecksIcon}
          title="No questions match"
          description="Try another topic or clear the search to see the whole bank."
        />
      )}

      <ul className="space-y-2">
        {questionsQuery.data?.items.map((question) => (
          <li key={question.id} className="rounded-lg border p-3">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm">{question.question_text}</p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {question.topic_name && (
                    <Badge variant="outline" asChild>
                      <Link to={`/prep/questions?topic_id=${question.topic_id}`}>{question.topic_name}</Link>
                    </Badge>
                  )}
                  <Badge variant="outline">{formatLabel(question.category)}</Badge>
                  {question.difficulty && <Badge variant="outline">{formatLabel(question.difficulty)}</Badge>}
                  {!question.is_system && <Badge variant="outline">Yours</Badge>}
                </div>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  aria-expanded={expandedId === question.id}
                  onClick={() => setExpandedId(expandedId === question.id ? null : question.id)}
                >
                  {expandedId === question.id ? <ChevronDownIcon className="h-4 w-4" /> : <ChevronRightIcon className="h-4 w-4" />}
                  History
                </Button>
                <Button size="sm" onClick={() => setLogging(question)}>
                  Log attempt
                </Button>
              </div>
            </div>
            {expandedId === question.id && <AttemptHistory questionId={question.id} />}
          </li>
        ))}
      </ul>

      {questionsQuery.data && total > PAGE_SIZE && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setParam('page', String(page - 1))}>
              Previous
            </Button>
            <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setParam('page', String(page + 1))}>
              Next
            </Button>
          </div>
        </div>
      )}

      <LogAttemptDialog
        key={logging?.id ?? 'none'}
        question={logging}
        isSaving={attemptMutation.isPending}
        onClose={() => setLogging(null)}
        onSubmit={(question, draft) => attemptMutation.mutate({ question, draft })}
      />
    </div>
  )
}

function AttemptHistory({ questionId }: { questionId: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['attempts', questionId],
    queryFn: () => listPracticeAttempts(questionId),
  })

  if (isLoading) {
    return <p className="mt-3 text-sm text-muted-foreground">Loading history</p>
  }
  if (isError) {
    return <p role="alert" className="mt-3 text-sm text-destructive">Couldn&apos;t load the history.</p>
  }
  if (!data || data.length === 0) {
    return <p className="mt-3 text-sm text-muted-foreground">No attempts yet.</p>
  }
  return (
    <ul className="mt-3 divide-y rounded-md border text-sm">
      {data.map((attempt) => (
        <li key={attempt.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
          <span>{formatLabel(attempt.status)}</span>
          <span className="text-muted-foreground">
            {attempt.confidence !== null ? `Confidence ${attempt.confidence}/5 · ` : ''}
            {format(new Date(attempt.attempted_at), 'MMM d, yyyy')}
          </span>
        </li>
      ))}
    </ul>
  )
}
