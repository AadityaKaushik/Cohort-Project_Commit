import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api-client'
import { listInterviews } from '@/lib/interviews-api'
import { COMPETENCIES, createStory, getStory, storyKeys, updateStory, type Competency, type StoryInput } from '@/lib/stories-api'
import { formatLabel } from '@/lib/labels'
import { hasErrors, meaningfulText, required, validate } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { CheckboxField, FormActions, FormError, FormField, FormSection } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { useUnsavedChanges } from '@/components/shared/UnsavedChangesGuard'

const NO_INTERVIEW = 'none'

const STAR_FIELDS = [
  { key: 'situation', label: 'Situation', hint: 'Where and when this happened.' },
  { key: 'task', label: 'Task', hint: 'What you were responsible for.' },
  { key: 'action', label: 'Action', hint: 'What you did, in your own words.' },
  { key: 'result', label: 'Result', hint: 'What changed, ideally with a number.' },
] as const

type StarKey = (typeof STAR_FIELDS)[number]['key']

export function StoryFormPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({ queryKey: storyKeys.detail(id ?? ''), queryFn: () => getStory(id!), enabled: isEditing })
  const interviewsQuery = useQuery({ queryKey: ['interviews', 'for-story-form'], queryFn: () => listInterviews({ size: 100 }) })

  const [title, setTitle] = useState('')
  const [star, setStar] = useState<Record<StarKey, string>>({ situation: '', task: '', action: '', result: '' })
  const [competencies, setCompetencies] = useState<Competency[]>([])
  const [interviewId, setInterviewId] = useState(searchParams.get('interview_id') ?? NO_INTERVIEW)
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const { guard, markSaved } = useUnsavedChanges(dirty)

  useEffect(() => {
    const story = detailQuery.data
    if (story) {
      setTitle(story.title)
      setStar({ situation: story.situation ?? '', task: story.task ?? '', action: story.action ?? '', result: story.result ?? '' })
      setCompetencies(story.competencies)
      setInterviewId(story.interview_id ?? NO_INTERVIEW)
    }
  }, [detailQuery.data])

  function change<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value)
      setDirty(true)
    }
  }

  function toggleCompetency(competency: Competency, checked: boolean) {
    setCompetencies((current) => (checked ? [...current, competency] : current.filter((item) => item !== competency)))
    setDirty(true)
  }

  const mutation = useMutation({
    mutationFn: () => {
      const input: StoryInput = {
        title: title.trim(),
        ...star,
        competencies,
        interview_id: interviewId === NO_INTERVIEW ? undefined : interviewId,
      }
      return isEditing ? updateStory(id!, input) : createStory(input)
    },
    onSuccess: (story) => {
      markSaved()
      queryClient.invalidateQueries({ queryKey: storyKeys.all })
      navigate(`/stories/${story.id}`)
    },
    onError: (mutationError: unknown) => {
      if (mutationError instanceof ApiError) {
        setError(mutationError.message)
        setFieldErrors(mutationError.fieldErrors ?? {})
      } else {
        setError('Something went wrong. Please try again.')
      }
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const problems = validate(
      { title, ...star },
      {
        title: [required('Title')],
        situation: [meaningfulText],
        task: [meaningfulText],
        action: [meaningfulText],
        result: [meaningfulText],
      },
    )
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate()
  }

  if (isEditing && detailQuery.isLoading) {
    return <PageSkeleton label="Loading story" variant="form" />
  }
  if (isEditing && detailQuery.isError) {
    return <ErrorState title="Couldn't load this story" description="It may have been deleted, or something went wrong." onRetry={() => detailQuery.refetch()} />
  }

  return (
    <div className="max-w-4xl space-y-6">
      {guard}
      <PageHeader
        back={isEditing ? { to: `/stories/${id}`, label: 'Story' } : { to: '/stories', label: 'Stories' }}
        title={isEditing ? 'Edit story' : 'New story'}
      />
      <SectionCard>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <FormSection title="Summary" description="A short name you will recognise." columns={1}>
            <FormField id="title" label="Title" error={fieldErrors.title}>
              <Input id="title" value={title} maxLength={200} onChange={(event) => change(setTitle)(event.target.value)} />
            </FormField>
          </FormSection>
          <FormSection title="The story" description="Situation, task, action, result." columns={1}>
            {STAR_FIELDS.map(({ key, label, hint }) => (
              <FormField key={key} id={key} label={label} hint={hint} error={fieldErrors[key]}>
                <Textarea id={key} rows={3} value={star[key]} onChange={(event) => change(setStar)({ ...star, [key]: event.target.value })} />
              </FormField>
            ))}
          </FormSection>
          <FormSection title="Tags" description="Which questions this story answers." columns={3}>
            {COMPETENCIES.map((competency) => (
              <CheckboxField
                key={competency}
                label={formatLabel(competency)}
                checked={competencies.includes(competency)}
                onChange={(checked) => toggleCompetency(competency, checked)}
              />
            ))}
          </FormSection>
          <FormSection title="Round" description="Optional. Link the story to a round." columns={1}>
            <FormField id="interview" label="Interview" error={fieldErrors.interviewId}>
              <Select value={interviewId} onValueChange={change(setInterviewId)}>
                <SelectTrigger id="interview" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NO_INTERVIEW}>No interview</SelectItem>
                  {interviewsQuery.data?.items.map((interview) => (
                    <SelectItem key={interview.id} value={interview.id}>
                      {interview.company_name} - round {interview.round_number}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormField>
          </FormSection>
          {error && <FormError>{error}</FormError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving...' : isEditing ? 'Save changes' : 'Save story'}
            </Button>
          </FormActions>
        </form>
      </SectionCard>
    </div>
  )
}
