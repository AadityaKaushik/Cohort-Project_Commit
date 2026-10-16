import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { PlayIcon } from 'lucide-react'
import {
  getMockInterview,
  recordPracticeAttempt,
  type MockInterviewQuestion,
  type PracticeAttemptStatus,
} from '@/lib/prep-api'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { FormError, FormField } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { SectionCard } from '@/components/shared/SectionCard'

const QUESTION_COUNTS = [3, 5, 8]
const MINUTES_PER_QUESTION = [5, 10, 15]

const RATINGS = [
  { value: 1, label: 'Could not answer' },
  { value: 2, label: 'Barely' },
  { value: 3, label: 'Partly' },
  { value: 4, label: 'Mostly' },
  { value: 5, label: 'Confident' },
]

interface Result {
  question: MockInterviewQuestion
  rating: number
  minutes: number
}

function statusForRating(rating: number): PracticeAttemptStatus {
  if (rating >= 4) return 'confident'
  if (rating === 3) return 'in_progress'
  return 'needs_review'
}

function formatClock(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function MockInterviewPage() {
  const queryClient = useQueryClient()
  const [count, setCount] = useState(5)
  const [minutesPerQuestion, setMinutesPerQuestion] = useState(10)
  const [questions, setQuestions] = useState<MockInterviewQuestion[] | null>(null)
  const [index, setIndex] = useState(0)
  const [results, setResults] = useState<Result[]>([])

  const startMutation = useMutation({
    mutationFn: () => getMockInterview(count),
    onSuccess: (data) => {
      setQuestions(data.questions)
      setIndex(0)
      setResults([])
    },
  })

  const finished = questions !== null && questions.length > 0 && index >= questions.length

  function handleAnswered(result: Result) {
    setResults((current) => [...current, result])
    setIndex((current) => current + 1)
    queryClient.invalidateQueries({ queryKey: ['preparation-progress'] })
    queryClient.invalidateQueries({ queryKey: ['review-queue'] })
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Mock interview"
        description="A timed session from your weakest topics. You rate your own answers."
        back={{ to: '/prep', label: 'Interview Prep' }}
      />

      {questions === null && (
        <SectionCard title="Set up the session">
          <div className="space-y-5">
            <OptionGroup label="Number of questions" options={QUESTION_COUNTS} value={count} onChange={setCount} />
            <OptionGroup
              label="Minutes per question"
              options={MINUTES_PER_QUESTION}
              value={minutesPerQuestion}
              onChange={setMinutesPerQuestion}
            />
            {startMutation.isError && <FormError>Could not load the questions. Try again.</FormError>}
            <Button onClick={() => startMutation.mutate()} disabled={startMutation.isPending}>
              <PlayIcon aria-hidden />
              Start
            </Button>
          </div>
        </SectionCard>
      )}

      {questions !== null && questions.length === 0 && (
        <SectionCard title="No questions available">
          <p className="text-sm text-muted-foreground">There are no questions to build a session from yet.</p>
          <Button asChild variant="outline" className="mt-4">
            <Link to="/prep">Back to Interview Prep</Link>
          </Button>
        </SectionCard>
      )}

      {questions !== null && index < questions.length && (
        <QuestionStep
          key={questions[index].question_id}
          question={questions[index]}
          position={index + 1}
          total={questions.length}
          limitSeconds={minutesPerQuestion * 60}
          isLast={index === questions.length - 1}
          onAnswered={handleAnswered}
        />
      )}

      {finished && <Summary results={results} />}
    </div>
  )
}

function OptionGroup({
  label,
  options,
  value,
  onChange,
}: {
  label: string
  options: number[]
  value: number
  onChange: (value: number) => void
}) {
  return (
    <div role="group" aria-label={label} className="space-y-2">
      <p className="text-sm font-medium">{label}</p>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => (
          <Button
            key={option}
            type="button"
            variant={option === value ? 'default' : 'outline'}
            aria-pressed={option === value}
            onClick={() => onChange(option)}
          >
            {option}
          </Button>
        ))}
      </div>
    </div>
  )
}

function QuestionStep({
  question,
  position,
  total,
  limitSeconds,
  isLast,
  onAnswered,
}: {
  question: MockInterviewQuestion
  position: number
  total: number
  limitSeconds: number
  isLast: boolean
  onAnswered: (result: Result) => void
}) {
  const [elapsed, setElapsed] = useState(0)
  const [rating, setRating] = useState<number | null>(null)
  const [notes, setNotes] = useState('')

  useEffect(() => {
    const timer = window.setInterval(() => setElapsed((current) => current + 1), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const remaining = Math.max(0, limitSeconds - elapsed)
  const minutes = Math.max(1, Math.ceil(elapsed / 60))

  const attemptMutation = useMutation({
    mutationFn: (selected: number) =>
      recordPracticeAttempt(question.question_id, {
        status: statusForRating(selected),
        confidence: selected,
        time_taken_minutes: minutes,
        notes: notes.trim() || null,
      }),
    onSuccess: (_data, selected) => onAnswered({ question, rating: selected, minutes }),
  })

  return (
    <SectionCard
      title={`Question ${position} of ${total}`}
      description={question.topic_name}
      action={
        <p className="text-sm tabular-nums" aria-label="Time left">
          {remaining === 0 ? <span className="font-medium text-destructive">Time is up</span> : formatClock(remaining)}
        </p>
      }
    >
      <div className="space-y-5">
        <p className="text-base">{question.question_text}</p>

        <FormField id="mock-notes" label="Notes">
          <Textarea id="mock-notes" rows={5} value={notes} onChange={(event) => setNotes(event.target.value)} />
        </FormField>

        <div role="group" aria-label="Rate your answer" className="space-y-2">
          <p className="text-sm font-medium">Outcome</p>
          <div className="flex flex-wrap gap-2">
            {RATINGS.map((option) => (
              <Button
                key={option.value}
                type="button"
                variant={rating === option.value ? 'default' : 'outline'}
                aria-pressed={rating === option.value}
                onClick={() => setRating(option.value)}
                className="h-auto flex-col gap-0.5 py-2"
              >
                <span>{option.value}</span>
                <span className="text-xs font-normal">{option.label}</span>
              </Button>
            ))}
          </div>
        </div>

        {attemptMutation.isError && <FormError>Could not save this answer. Try again.</FormError>}

        <Button onClick={() => rating !== null && attemptMutation.mutate(rating)} disabled={rating === null || attemptMutation.isPending}>
          {isLast ? 'Finish' : 'Next'}
        </Button>
      </div>
    </SectionCard>
  )
}

function Summary({ results }: { results: Result[] }) {
  return (
    <SectionCard
      title="Session summary"
      action={
        <Button asChild variant="outline">
          <Link to="/prep">Back to Interview Prep</Link>
        </Button>
      }
    >
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Question</TableHead>
            <TableHead>Topic</TableHead>
            <TableHead>Rating</TableHead>
            <TableHead>Minutes</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {results.map((result) => (
            <TableRow key={result.question.question_id}>
              <TableCell className="whitespace-normal">{result.question.question_text}</TableCell>
              <TableCell>{result.question.topic_name}</TableCell>
              <TableCell>{result.rating}</TableCell>
              <TableCell>{result.minutes}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </SectionCard>
  )
}
