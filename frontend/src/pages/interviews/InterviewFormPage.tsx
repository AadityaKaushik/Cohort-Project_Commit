import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import {
  createInterview,
  getInterview,
  updateInterview,
  type InterviewMode,
  type InterviewRoundType,
} from '@/lib/interviews-api'
import { listApplications } from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
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
    return (
      <div className="max-w-2xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (isEditing && detailQuery.isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load this interview</AlertTitle>
        <AlertDescription>It may have been deleted, or something went wrong.</AlertDescription>
      </Alert>
    )
  }

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>{isEditing ? 'Edit interview' : 'Record an interview'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {!isEditing && (
            <div className="space-y-2">
              <Label htmlFor="application">Application</Label>
              <Select value={applicationId} onValueChange={setApplicationId}>
                <SelectTrigger id="application">
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
              {fieldErrors.applicationId && <p className="text-sm text-destructive">{fieldErrors.applicationId}</p>}
            </div>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="roundNumber">Round number</Label>
              <Input
                id="roundNumber"
                type="number"
                min={1}
                value={roundNumber}
                onChange={(event) => setRoundNumber(event.target.value)}
                required
              />
              {fieldErrors.roundNumber && <p className="text-sm text-destructive">{fieldErrors.roundNumber}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="roundType">Round type</Label>
              <Select value={roundType} onValueChange={(value) => setRoundType(value as InterviewRoundType)}>
                <SelectTrigger id="roundType">
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
            </div>
            <div className="space-y-2">
              <Label htmlFor="scheduledAt">Scheduled at</Label>
              <Input
                id="scheduledAt"
                type="datetime-local"
                value={scheduledAt}
                onChange={(event) => setScheduledAt(event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="mode">Mode</Label>
              <Select value={mode} onValueChange={(value) => setMode(value as InterviewMode)}>
                <SelectTrigger id="mode">
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
            </div>
            <div className="space-y-2">
              <Label htmlFor="durationMinutes">Duration (minutes)</Label>
              <Input
                id="durationMinutes"
                type="number"
                min={0}
                value={durationMinutes}
                onChange={(event) => setDurationMinutes(event.target.value)}
              />
              {fieldErrors.durationMinutes && <p className="text-sm text-destructive">{fieldErrors.durationMinutes}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="interviewerName">Interviewer name</Label>
              <Input id="interviewerName" value={interviewerName} onChange={(event) => setInterviewerName(event.target.value)} />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea id="notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} />
          </div>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Record interview'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
