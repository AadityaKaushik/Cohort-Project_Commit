import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import {
  createApplication,
  getApplication,
  updateApplication,
  type ApplicationFormInput,
  type ApplicationStatusValue,
  type JobType,
  type Priority,
  type WorkMode,
} from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { email as emailRule, hasErrors, positiveInteger, required, url, validate } from '@/lib/validation'
import { useToast } from '@/hooks/use-toast'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'

const CREATE_STATUS_OPTIONS: ApplicationStatusValue[] = ['SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'FINAL_ROUND', 'OFFER']
const WORK_MODES: WorkMode[] = ['onsite', 'hybrid', 'remote']
const JOB_TYPES: JobType[] = ['internship', 'full_time', 'part_time', 'contract']
const PRIORITIES: Priority[] = ['low', 'medium', 'high']

interface FormState {
  companyName: string
  jobTitle: string
  dateApplied: string
  status: ApplicationStatusValue
  location: string
  workMode: WorkMode | ''
  jobType: JobType | ''
  jobUrl: string
  jobDescription: string
  salaryMin: string
  salaryMax: string
  salaryCurrency: string
  priority: Priority | ''
  source: string
  isReferral: boolean
  deadline: string
  recruiterName: string
  recruiterEmail: string
  notes: string
  coverLetter: string
}

const EMPTY_FORM: FormState = {
  companyName: '',
  jobTitle: '',
  dateApplied: '',
  status: 'SAVED',
  location: '',
  workMode: '',
  jobType: '',
  jobUrl: '',
  jobDescription: '',
  salaryMin: '',
  salaryMax: '',
  salaryCurrency: '',
  priority: '',
  source: '',
  isReferral: false,
  deadline: '',
  recruiterName: '',
  recruiterEmail: '',
  notes: '',
  coverLetter: '',
}

export function ApplicationFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const detailQuery = useQuery({
    queryKey: ['application', id],
    queryFn: () => getApplication(id!),
    enabled: isEditing,
  })

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  useEffect(() => {
    const data = detailQuery.data
    if (data) {
      setForm({
        companyName: data.company.name,
        jobTitle: data.job_title,
        dateApplied: data.date_applied,
        status: data.status,
        location: data.location ?? '',
        workMode: data.work_mode ?? '',
        jobType: data.job_type ?? '',
        jobUrl: data.job_url ?? '',
        jobDescription: data.job_description ?? '',
        salaryMin: data.salary_min === null ? '' : String(data.salary_min),
        salaryMax: data.salary_max === null ? '' : String(data.salary_max),
        salaryCurrency: data.salary_currency ?? '',
        priority: data.priority ?? '',
        source: data.source ?? '',
        isReferral: data.is_referral,
        deadline: data.deadline ?? '',
        recruiterName: data.recruiter_name ?? '',
        recruiterEmail: data.recruiter_email ?? '',
        notes: data.notes ?? '',
        coverLetter: data.cover_letter ?? '',
      })
    }
  }, [detailQuery.data])

  const mutation = useMutation({
    mutationFn: (input: ApplicationFormInput) => (isEditing ? updateApplication(id!, input) : createApplication(input)),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['application', result.id] })
      toast({ title: isEditing ? 'Application updated' : 'Application created' })
      navigate(`/applications/${result.id}`)
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
      {
        companyName: form.companyName,
        jobTitle: form.jobTitle,
        dateApplied: form.dateApplied,
        jobUrl: form.jobUrl,
        salaryMin: form.salaryMin,
        salaryMax: form.salaryMax,
        salaryCurrency: form.salaryCurrency,
        recruiterEmail: form.recruiterEmail,
      },
      {
        companyName: [required('Company')],
        jobTitle: [required('Job title')],
        dateApplied: [required('Date applied')],
        jobUrl: [url],
        salaryMin: [positiveInteger('Minimum salary')],
        salaryMax: [positiveInteger('Maximum salary')],
        salaryCurrency: [(value) => (value === '' || /^[A-Za-z]{3}$/.test(value) ? null : 'Use a three-letter currency code, like USD.')],
        recruiterEmail: [emailRule],
      },
    )
    if (form.salaryMin && form.salaryMax && Number(form.salaryMin) > Number(form.salaryMax) && !problems.salaryMax) {
      problems.salaryMax = 'Maximum salary must be at least the minimum.'
    }
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate({
      company_name: form.companyName,
      job_title: form.jobTitle,
      date_applied: form.dateApplied,
      status: isEditing ? undefined : form.status,
      location: form.location || null,
      work_mode: form.workMode || null,
      job_type: form.jobType || null,
      job_url: form.jobUrl || null,
      job_description: form.jobDescription || null,
      salary_min: form.salaryMin ? Number(form.salaryMin) : null,
      salary_max: form.salaryMax ? Number(form.salaryMax) : null,
      salary_currency: form.salaryCurrency ? form.salaryCurrency.toUpperCase() : null,
      priority: form.priority || null,
      source: form.source || null,
      is_referral: form.isReferral,
      deadline: form.deadline || null,
      recruiter_name: form.recruiterName || null,
      recruiter_email: form.recruiterEmail || null,
      notes: form.notes || null,
      cover_letter: form.coverLetter || null,
    })
  }

  if (isEditing && detailQuery.isLoading) {
    return (
      <div className="max-w-3xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (isEditing && detailQuery.isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load this application</AlertTitle>
        <AlertDescription>It may have been deleted, or something went wrong.</AlertDescription>
      </Alert>
    )
  }

  function field(key: string) {
    return fieldErrors[key] ? <p className="text-sm text-destructive">{fieldErrors[key]}</p> : null
  }

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <CardTitle>{isEditing ? 'Edit application' : 'Create an application'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-2 text-sm font-medium">The role</legend>
            <div className="space-y-2">
              <Label htmlFor="companyName">Company</Label>
              <Input id="companyName" value={form.companyName} onChange={(e) => set('companyName', e.target.value)} required />
              {field('companyName')}
            </div>
            <div className="space-y-2">
              <Label htmlFor="jobTitle">Job title</Label>
              <Input id="jobTitle" value={form.jobTitle} onChange={(e) => set('jobTitle', e.target.value)} required />
              {field('jobTitle')}
            </div>
            <div className="space-y-2">
              <Label htmlFor="dateApplied">Date applied</Label>
              <Input id="dateApplied" type="date" value={form.dateApplied} onChange={(e) => set('dateApplied', e.target.value)} required />
              {field('dateApplied')}
            </div>
            {!isEditing && (
              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <NativeSelect id="status" value={form.status} onChange={(e) => set('status', e.target.value as ApplicationStatusValue)}>
                  {CREATE_STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {formatLabel(option)}
                    </option>
                  ))}
                </NativeSelect>
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              <Input id="location" value={form.location} onChange={(e) => set('location', e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="workMode">Work mode</Label>
              <NativeSelect id="workMode" value={form.workMode} onChange={(e) => set('workMode', e.target.value as WorkMode | '')}>
                <option value="">Not specified</option>
                {WORK_MODES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </div>
            <div className="space-y-2">
              <Label htmlFor="jobType">Job type</Label>
              <NativeSelect id="jobType" value={form.jobType} onChange={(e) => set('jobType', e.target.value as JobType | '')}>
                <option value="">Not specified</option>
                {JOB_TYPES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </div>
            <div className="space-y-2">
              <Label htmlFor="jobUrl">Job URL</Label>
              <Input id="jobUrl" type="url" value={form.jobUrl} onChange={(e) => set('jobUrl', e.target.value)} />
              {field('jobUrl')}
            </div>
          </fieldset>

          <fieldset className="grid gap-4 sm:grid-cols-3">
            <legend className="mb-2 text-sm font-medium">Compensation</legend>
            <div className="space-y-2">
              <Label htmlFor="salaryMin">Minimum salary</Label>
              <Input id="salaryMin" inputMode="numeric" value={form.salaryMin} onChange={(e) => set('salaryMin', e.target.value)} />
              {field('salaryMin')}
            </div>
            <div className="space-y-2">
              <Label htmlFor="salaryMax">Maximum salary</Label>
              <Input id="salaryMax" inputMode="numeric" value={form.salaryMax} onChange={(e) => set('salaryMax', e.target.value)} />
              {field('salaryMax')}
            </div>
            <div className="space-y-2">
              <Label htmlFor="salaryCurrency">Currency</Label>
              <Input id="salaryCurrency" maxLength={3} placeholder="USD" value={form.salaryCurrency} onChange={(e) => set('salaryCurrency', e.target.value)} />
              {field('salaryCurrency')}
            </div>
          </fieldset>

          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-2 text-sm font-medium">Tracking</legend>
            <div className="space-y-2">
              <Label htmlFor="priority">Priority</Label>
              <NativeSelect id="priority" value={form.priority} onChange={(e) => set('priority', e.target.value as Priority | '')}>
                <option value="">Not set</option>
                {PRIORITIES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </div>
            <div className="space-y-2">
              <Label htmlFor="deadline">Deadline</Label>
              <Input id="deadline" type="date" value={form.deadline} onChange={(e) => set('deadline', e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="source">Source</Label>
              <Input id="source" placeholder="LinkedIn, referral, careers page" value={form.source} onChange={(e) => set('source', e.target.value)} />
            </div>
            <label className="flex items-center gap-2 self-end pb-2 text-sm">
              <input
                type="checkbox"
                checked={form.isReferral}
                onChange={(e) => set('isReferral', e.target.checked)}
                className="h-4 w-4 rounded border-input"
              />
              I was referred
            </label>
          </fieldset>

          <fieldset className="grid gap-4 sm:grid-cols-2">
            <legend className="mb-2 text-sm font-medium">Recruiter</legend>
            <div className="space-y-2">
              <Label htmlFor="recruiterName">Recruiter name</Label>
              <Input id="recruiterName" value={form.recruiterName} onChange={(e) => set('recruiterName', e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="recruiterEmail">Recruiter email</Label>
              <Input id="recruiterEmail" type="email" value={form.recruiterEmail} onChange={(e) => set('recruiterEmail', e.target.value)} />
              {field('recruiterEmail')}
            </div>
          </fieldset>

          <fieldset className="space-y-4">
            <legend className="mb-2 text-sm font-medium">Notes and documents</legend>
            <div className="space-y-2">
              <Label htmlFor="jobDescription">Job description</Label>
              <Textarea id="jobDescription" value={form.jobDescription} onChange={(e) => set('jobDescription', e.target.value)} rows={4} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="notes">Notes</Label>
              <Textarea id="notes" value={form.notes} onChange={(e) => set('notes', e.target.value)} rows={3} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="coverLetter">Cover letter</Label>
              <Textarea id="coverLetter" value={form.coverLetter} onChange={(e) => set('coverLetter', e.target.value)} rows={4} />
            </div>
          </fieldset>

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
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create application'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
