import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BookmarkPlusIcon, CheckIcon } from 'lucide-react'
import { saveInterviewQuestionToBank, type InterviewQuestion } from '@/lib/interviews-api'
import { listTopics } from '@/lib/prep-api'
import { ApiError } from '@/lib/api-client'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

interface SaveToBankControlProps {
  question: InterviewQuestion
  interviewId: string
}

/** Copies a question from a real interview into the user's own question bank so it can be practised. */
export function SaveToBankControl({ question, interviewId }: SaveToBankControlProps) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [choosingTopic, setChoosingTopic] = useState(false)
  const [topicId, setTopicId] = useState('')
  const [topicError, setTopicError] = useState<string | null>(null)

  const topicsQuery = useQuery({ queryKey: ['topics'], queryFn: listTopics, enabled: choosingTopic })

  const mutation = useMutation({
    mutationFn: (chosenTopic: string | null) => saveInterviewQuestionToBank(question.id, chosenTopic),
    onSuccess: () => {
      setChoosingTopic(false)
      queryClient.invalidateQueries({ queryKey: ['interview-questions', interviewId] })
      queryClient.invalidateQueries({ queryKey: ['questions'] })
      toast({ title: 'Added to your question bank' })
    },
    onError: (error: unknown) => {
      const fieldError = error instanceof ApiError ? error.fieldErrors?.topicId : undefined
      if (fieldError) {
        setChoosingTopic(true)
        setTopicError(fieldError)
        return
      }
      toast({ title: 'Could not add the question', variant: 'error' })
    },
  })

  if (question.linked_question_id) {
    return (
      <Link to="/prep/questions" className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground">
        <CheckIcon aria-hidden className="size-3.5" />
        In your question bank
      </Link>
    )
  }

  if (!choosingTopic) {
    return (
      <Button
        variant="outline"
        size="sm"
        disabled={mutation.isPending}
        onClick={() => (question.topic_id ? mutation.mutate(null) : setChoosingTopic(true))}
      >
        <BookmarkPlusIcon aria-hidden />
        Add to question bank
      </Button>
    )
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select value={topicId} onValueChange={(value) => { setTopicId(value); setTopicError(null) }}>
        <SelectTrigger aria-label="Topic" aria-invalid={Boolean(topicError)} className="w-[200px]">
          <SelectValue placeholder="Pick a topic" />
        </SelectTrigger>
        <SelectContent>
          {topicsQuery.data?.map((topic) => (
            <SelectItem key={topic.id} value={topic.id}>
              {topic.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button size="sm" disabled={!topicId || mutation.isPending} onClick={() => mutation.mutate(topicId)}>
        Save
      </Button>
      <Button variant="ghost" size="sm" onClick={() => setChoosingTopic(false)}>
        Cancel
      </Button>
      {topicError && <p className="w-full text-xs text-destructive">{topicError}</p>}
    </div>
  )
}
