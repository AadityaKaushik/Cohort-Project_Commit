import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDownIcon, ChevronRightIcon, ListChecksIcon, SearchIcon } from 'lucide-react'
import { format } from 'date-fns'
import {
  getPreparationProgress,
  listPracticeAttempts,
  listQuestions,
  listTopics,
  getQuestion,
  recordPracticeAttempt,
  type Difficulty,
  type QuestionBankItem,
  type QuestionCategory,
} from '@/lib/prep-api'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { useToast } from '@/hooks/use-toast'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { EmptyState } from '@/components/shared/EmptyState'
import { Pagination } from '@/components/shared/DataTable'
import { PageHeader } from '@/components/shared/PageHeader'
import { ToneBadge, type Tone } from '@/components/shared/ToneBadge'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { LogAttemptDialog, type AttemptDraft } from '@/components/prep/LogAttemptDialog'
import { MasteryBadge } from '@/components/prep/MasteryBadge'

const CATEGORIES: QuestionCategory[] = ['dsa', 'technical', 'behavioural', 'hr', 'system_design', 'company_specific']
const DIFFICULTIES: Difficulty[] = ['easy', 'medium', 'hard']
const PAGE_SIZE = 20
const DIFFICULTY_TONES: Record<Difficulty, Tone> = { easy: 'success', medium: 'warning', hard: 'danger' }

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

  const practiseId = params.get('practise') ?? ''
  const practiseQuery = useQuery({
    queryKey: ['question', practiseId],
    queryFn: () => getQuestion(practiseId),
    enabled: practiseId !== '',
  })
  const practiseQuestion = practiseQuery.data

  useEffect(() => {
    if (practiseId === '' || (!practiseQuestion && !practiseQuery.isError)) {
      return
    }
    if (practiseQuestion) {
      setLogging(practiseQuestion)
    }
    const next = new URLSearchParams(params)
    next.delete('practise')
    setParams(next, { replace: true })
  }, [practiseId, practiseQuestion, practiseQuery.isError, params, setParams])

  const attemptMutation = useMutation({
    mutationFn: ({ question, draft }: { question: QuestionBankItem; draft: AttemptDraft }) =>
      recordPracticeAttempt(question.id, draft),
    onSuccess: async (_attempt, { question }) => {
      setLogging(null)
      queryClient.invalidateQueries({ queryKey: ['attempts', question.id] })
      queryClient.invalidateQueries({ queryKey: ['review-queue'] })
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
      <PageHeader
        back={{ to: '/prep', label: 'Interview Prep' }}
        title={selectedTopic ? selectedTopic.name : 'Question bank'}
        description="Log every attempt. Mastery for a topic moves as your confidence and recency change."
        meta={
          topicMastery && (
            <MasteryBadge
              masteryScore={topicMastery.mastery_score}
              sufficientEvidence={topicMastery.sufficient_evidence}
              observationCount={topicMastery.observation_count}
            />
          )
        }
      />

      <form
        role="search"
        aria-label="Filter questions"
        className="surface-raised grid grid-cols-2 gap-2 rounded-xl border p-2 sm:flex sm:flex-wrap sm:items-center"
        onSubmit={(e) => e.preventDefault()}
      >
        <div className="relative col-span-2 min-w-48 flex-1">
          <SearchIcon aria-hidden className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <label htmlFor="bank-q" className="sr-only">
            Search questions
          </label>
          <Input id="bank-q" type="search" className="pl-8" placeholder="Search questions" value={q} onChange={(e) => setParam('q', e.target.value)} />
        </div>
        <div className="min-w-0 sm:w-44">
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
        <div className="min-w-0 sm:w-44">
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
        <div className="col-span-2 min-w-0 sm:col-span-1 sm:w-40">
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
      {questionsQuery.isError && <ErrorState title="Couldn't load questions" error={questionsQuery.error} onRetry={() => questionsQuery.refetch()} />}
      {questionsQuery.data && questionsQuery.data.items.length === 0 && (
        <EmptyState
          compact
          icon={ListChecksIcon}
          title="No questions match"
          description="Try another topic or clear the search to see the whole bank."
          action={
            <Button variant="outline" onClick={() => setParams(new URLSearchParams())}>
              Clear filters
            </Button>
          }
        />
      )}

      <ul className="space-y-2">
        {questionsQuery.data?.items.map((question) => (
          <li key={question.id} className="surface-raised rounded-xl border p-4 transition-colors hover:border-primary/25">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-sm leading-relaxed font-medium">{question.question_text}</p>
                <div className="mt-2.5 flex flex-wrap gap-1.5">
                  {question.topic_name && (
                    <ToneBadge tone="primary" asChild>
                      <Link to={`/prep/questions?topic_id=${question.topic_id}`}>{question.topic_name}</Link>
                    </ToneBadge>
                  )}
                  <ToneBadge tone="neutral">{formatLabel(question.category)}</ToneBadge>
                  {question.difficulty && <ToneBadge tone={DIFFICULTY_TONES[question.difficulty]}>{formatLabel(question.difficulty)}</ToneBadge>}
                  {!question.is_system && <ToneBadge tone="info">Yours</ToneBadge>}
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
        <Pagination page={page} totalPages={totalPages} onPageChange={(next) => setParam('page', String(next))} label="Question pages" />
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
    return <Skeleton role="status" aria-label="Loading history" className="mt-3 h-10 w-full rounded-lg" />
  }
  if (isError) {
    return <p role="alert" className="mt-3 text-sm text-destructive">Couldn&apos;t load the history.</p>
  }
  if (!data || data.length === 0) {
    return <p className="mt-3 text-sm text-muted-foreground">No attempts yet.</p>
  }
  return (
    <ul className="mt-3 divide-y rounded-lg border bg-surface-2/50 text-sm">
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
