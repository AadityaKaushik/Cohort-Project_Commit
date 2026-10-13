import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createInterview,
  getInterview,
  updateInterview,
  type InterviewMode,
  type InterviewRoundType,
} from '@/lib/interviews-api'
import { listApplications } from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { FormActions, FormError, FormField, FormSection } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { formatLabel } from '@/lib/labels'
import { hasErrors, positiveInteger, required, validate } from '@/lib/validation'

const ROUND_TYPES: InterviewRoundType[] = ['OA', 'PHONE_SCREEN', 'TECHNICAL', 'SYSTEM_DESIGN', 'BEHAVIOURAL', 'HR', 'FINAL']
const MODES: InterviewMode[] = ['onsite', 'video', 'phone']

export function InterviewFormPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['interview', id],
    queryFn: () => getInterview(id!),
    enabled: isEditing,
  })

  const applicationsQuery = useQuery({
    queryKey: ['applications', 'for-interview-form'],
    queryFn: () => listApplications({ size: 100 }),
    enabled: !isEditing,
  })

  const [applicationId, setApplicationId] = useState(searchParams.get('application_id') ?? '')
  const [scheduledAt, setScheduledAt] = useState('')
  const [roundNumber, setRoundNumber] = useState('1')
  const [roundType, setRoundType] = useState<InterviewRoundType>('PHONE_SCREEN')
  const [mode, setMode] = useState<InterviewMode | ''>('')
  const [durationMinutes, setDurationMinutes] = useState('')
  const [interviewerName, setInterviewerName] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (detailQuery.data) {
      setScheduledAt(detailQuery.data.scheduled_at ? detailQuery.data.scheduled_at.slice(0, 16) : '')
      setRoundNumber(String(detailQuery.data.round_number))
      setRoundType(detailQuery.data.round_type)
      setMode(detailQuery.data.mode ?? '')
      setDurationMinutes(detailQuery.data.duration_minutes ? String(detailQuery.data.duration_minutes) : '')
      setInterviewerName(detailQuery.data.interviewer_name ?? '')
      setNotes(detailQuery.data.notes ?? '')
    }
  }, [detailQuery.data])

  const mutation = useMutation({
    mutationFn: () =>
      isEditing
        ? updateInterview(id!, {
            scheduled_at: scheduledAt ? new Date(scheduledAt).toISOString() : null,
            round_number: Number(roundNumber),
            round_type: roundType,
            mode: mode || null,
            duration_minutes: durationMinutes ? Number(durationMinutes) : null,
            interviewer_name: interviewerName || null,
            notes: notes || null,
          })
        : createInterview({
            application_id: applicationId,
            scheduled_at: scheduledAt ? new Date(scheduledAt).toISOString() : null,
            round_number: Number(roundNumber),
            round_type: roundType,
            mode: mode || null,
            duration_minutes: durationMinutes ? Number(durationMinutes) : null,
            interviewer_name: interviewerName || null,
            notes: notes || null,
          }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['interviews'] })
      queryClient.invalidateQueries({ queryKey: ['interview', result.id] })
      queryClient.invalidateQueries({ queryKey: ['application', result.application_id] })
      navigate(`/interviews/${result.id}`)
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
      { applicationId: isEditing ? 'existing' : applicationId, roundNumber, durationMinutes },
      {
        applicationId: [required('Application')],
        roundNumber: [required('Round number'), positiveInteger('Round number')],
        durationMinutes: [positiveInteger('Duration')],
      },
    )
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate()
  }

  if (isEditing && detailQuery.isLoading) {
    return <PageSkeleton label="Loading interview" variant="form" />
  }

  if (isEditing && detailQuery.isError) {
    return (
      <ErrorState title="Couldn't load this interview" description="It may have been deleted, or something went wrong." onRetry={() => detailQuery.refetch()} />
    )
  }

  return (
    <div className="max-w-4xl space-y-6">
      <PageHeader
        back={isEditing ? { to: `/interviews/${id}`, label: 'Interview' } : { to: '/interviews', label: 'Interviews' }}
        title={isEditing ? 'Edit interview' : 'Record an interview'}
        description="Schedule the round now; the debrief prompt follows once it has happened."
      />
      <SectionCard>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <FormSection title="The round" description="Which application it belongs to and what kind of round it is.">
            {!isEditing && (
              <FormField id="application" label="Application" error={fieldErrors.applicationId} className="sm:col-span-2">
                <Select value={applicationId} onValueChange={setApplicationId}>
                  <SelectTrigger id="application" className="w-full">
                    <SelectValue placeholder="Choose an application" />
                  </SelectTrigger>
                  <SelectContent>
                    {applicationsQuery.data?.items.map((application) => (
                      <SelectItem key={application.id} value={application.id}>
                        {application.company.name} — {application.job_title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormField>
            )}
            <FormField id="roundNumber" label="Round number" error={fieldErrors.roundNumber}>
              <Input id="roundNumber" type="number" min={1} value={roundNumber} onChange={(event) => setRoundNumber(event.target.value)} required />
            </FormField>
            <FormField id="roundType" label="Round type">
              <Select value={roundType} onValueChange={(value) => setRoundType(value as InterviewRoundType)}>
                <SelectTrigger id="roundType" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROUND_TYPES.map((type) => (
                    <SelectItem key={type} value={type}>
                      {formatLabel(type)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormField>
          </FormSection>
          <FormSection title="Logistics" description="When, how and with whom.">
            <FormField id="scheduledAt" label="Scheduled at">
              <Input id="scheduledAt" type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} />
            </FormField>
            <FormField id="mode" label="Mode">
              <Select value={mode} onValueChange={(value) => setMode(value as InterviewMode)}>
                <SelectTrigger id="mode" className="w-full">
                  <SelectValue placeholder="Not specified" />
                </SelectTrigger>
                <SelectContent>
                  {MODES.map((m) => (
                    <SelectItem key={m} value={m}>
                      {formatLabel(m)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormField>
            <FormField id="durationMinutes" label="Duration (minutes)" error={fieldErrors.durationMinutes}>
              <Input id="durationMinutes" type="number" min={0} value={durationMinutes} onChange={(event) => setDurationMinutes(event.target.value)} />
            </FormField>
            <FormField id="interviewerName" label="Interviewer name">
              <Input id="interviewerName" value={interviewerName} onChange={(event) => setInterviewerName(event.target.value)} />
            </FormField>
          </FormSection>
          <FormSection title="Notes" description="Anything to prepare or remember." columns={1}>
            <FormField id="notes" label="Notes">
              <Textarea id="notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} />
            </FormField>
          </FormSection>
          {error && <FormError>{error}</FormError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Record interview'}
            </Button>
          </FormActions>
        </form>
      </SectionCard>
    </div>
  )
}
