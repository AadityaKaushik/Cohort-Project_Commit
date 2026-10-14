import { useEffect, useState, type ClipboardEvent, type FormEvent } from 'react'
import { format } from 'date-fns'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createApplication,
  getApplication,
  listApplications,
  updateApplication,
  type ApplicationDetail,
  type ApplicationFormInput,
  type ApplicationStatusValue,
  type JobType,
  type Priority,
  type WorkMode,
} from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { guessCompanyFromUrl } from '@/lib/company-guess'
import { listCompanies } from '@/lib/companies-api'
import { formatLabel } from '@/lib/labels'
import { email as emailRule, hasErrors, positiveInteger, required, url, validate } from '@/lib/validation'
import { useToast } from '@/hooks/use-toast'
import { clearDraft, readDraft, writeDraft } from '@/lib/drafts'
import { useUnsavedChanges } from '@/components/shared/UnsavedChangesGuard'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { CheckboxField, FormActions, FormError, FormField, FormSection } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'

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
  status: 'APPLIED',
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

function toFormState(data: ApplicationDetail): FormState {
  return {
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
  }
}

export function ApplicationFormPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const duplicateId = searchParams.get('from')
  const isEditing = Boolean(id)
  const sourceId = id ?? duplicateId
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const detailQuery = useQuery({
    queryKey: ['application', sourceId],
    queryFn: () => getApplication(sourceId!),
    enabled: Boolean(sourceId),
  })

  const lastUsedQuery = useQuery({
    queryKey: ['applications', 'last-used'],
    queryFn: async () => {
      const latest = await listApplications({ size: 1, sort: 'date_applied', direction: 'desc' })
      return latest.items[0] ? getApplication(latest.items[0].id) : null
    },
    enabled: !sourceId,
  })

  const companiesQuery = useQuery({ queryKey: ['companies', 'list', ''], queryFn: () => listCompanies('') })

  const draftKey = 'application-new'
  const today = format(new Date(), 'yyyy-MM-dd')
  const [form, setForm] = useState<FormState>(() => {
    const start = { ...EMPTY_FORM, dateApplied: today }
    return sourceId ? start : { ...start, ...readDraft<Partial<FormState>>(draftKey) }
  })
  const [baseline, setBaseline] = useState<FormState>({ ...EMPTY_FORM, dateApplied: today })
  const dirty = JSON.stringify(form) !== JSON.stringify(baseline)
  const { guard, markSaved } = useUnsavedChanges(dirty)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  useEffect(() => {
    const data = detailQuery.data
    if (!data) return
    const loaded = toFormState(data)
    const next = isEditing ? loaded : { ...loaded, status: 'APPLIED' as const, dateApplied: today }
    setForm(next)
    setBaseline(next)
  }, [detailQuery.data, isEditing, today])

  useEffect(() => {
    const last = lastUsedQuery.data
    if (!last) return
    const fill = (current: FormState): FormState => ({
      ...current,
      source: current.source || (last.source ?? ''),
      location: current.location || (last.location ?? ''),
      workMode: current.workMode || (last.work_mode ?? ''),
    })
    setForm(fill)
    setBaseline(fill)
  }, [lastUsedQuery.data])

  function handleUrlPaste(event: ClipboardEvent<HTMLInputElement>) {
    const guess = guessCompanyFromUrl(event.clipboardData.getData('text'))
    if (guess && form.companyName.trim() === '') {
      set('companyName', guess)
    }
  }

  const mutation = useMutation({
    mutationFn: (input: ApplicationFormInput) => (isEditing ? updateApplication(id!, input) : createApplication(input)),
    onSuccess: (result) => {
      markSaved()
      clearDraft(draftKey)
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

  useEffect(() => {
    if (!isEditing && dirty) {
      writeDraft(draftKey, form)
    }
  }, [isEditing, dirty, form])

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

  if (sourceId && detailQuery.isLoading) {
    return <PageSkeleton label="Loading application" variant="form" />
  }

  if (sourceId && detailQuery.isError) {
    return (
      <ErrorState
        title="Couldn't load this application"
        description="It may have been deleted, or something went wrong."
        onRetry={() => detailQuery.refetch()}
      />
    )
  }

  const backTo = isEditing ? { to: `/applications/${id}`, label: 'Application' } : { to: '/applications', label: 'Applications' }

  return (
    <div className="max-w-4xl space-y-6">
      {guard}
      <PageHeader
        back={backTo}
        title={isEditing ? 'Edit application' : duplicateId ? 'Duplicate application' : 'Create an application'}
        description={isEditing ? 'Update the details you track for this role.' : 'Only the company, role and date are required. Add the rest whenever you have it.'}
      />
      <SectionCard>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <FormSection title="The role" description="What you applied for and where it stands.">
            <FormField id="companyName" label="Company" error={fieldErrors.companyName}>
              <Input id="companyName" list="company-options" autoComplete="off" value={form.companyName} onChange={(e) => set('companyName', e.target.value)} required />
            </FormField>
            <datalist id="company-options">
              {companiesQuery.data?.map((company) => (
                <option key={company.id} value={company.name} />
              ))}
            </datalist>
            <FormField id="jobTitle" label="Job title" error={fieldErrors.jobTitle}>
              <Input id="jobTitle" value={form.jobTitle} onChange={(e) => set('jobTitle', e.target.value)} required />
            </FormField>
            <FormField id="dateApplied" label="Date applied" error={fieldErrors.dateApplied}>
              <Input id="dateApplied" type="date" value={form.dateApplied} onChange={(e) => set('dateApplied', e.target.value)} required />
            </FormField>
            {!isEditing && (
              <FormField id="status" label="Status">
                <NativeSelect id="status" value={form.status} onChange={(e) => set('status', e.target.value as ApplicationStatusValue)}>
                  {CREATE_STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {formatLabel(option)}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
            )}
            <FormField id="location" label="Location">
              <Input id="location" value={form.location} onChange={(e) => set('location', e.target.value)} />
            </FormField>
            <FormField id="workMode" label="Work mode">
              <NativeSelect id="workMode" value={form.workMode} onChange={(e) => set('workMode', e.target.value as WorkMode | '')}>
                <option value="">Not specified</option>
                {WORK_MODES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id="jobType" label="Job type">
              <NativeSelect id="jobType" value={form.jobType} onChange={(e) => set('jobType', e.target.value as JobType | '')}>
                <option value="">Not specified</option>
                {JOB_TYPES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id="jobUrl" label="Job URL" error={fieldErrors.jobUrl}>
              <Input id="jobUrl" type="url" placeholder="https://" value={form.jobUrl} onPaste={handleUrlPaste} onChange={(e) => set('jobUrl', e.target.value)} />
            </FormField>
          </FormSection>

          <FormSection title="Compensation" description="Whole numbers, in the posting's currency." columns={3}>
            <FormField id="salaryMin" label="Minimum salary" error={fieldErrors.salaryMin}>
              <Input id="salaryMin" inputMode="numeric" value={form.salaryMin} onChange={(e) => set('salaryMin', e.target.value)} />
            </FormField>
            <FormField id="salaryMax" label="Maximum salary" error={fieldErrors.salaryMax}>
              <Input id="salaryMax" inputMode="numeric" value={form.salaryMax} onChange={(e) => set('salaryMax', e.target.value)} />
            </FormField>
            <FormField id="salaryCurrency" label="Currency" error={fieldErrors.salaryCurrency}>
              <Input id="salaryCurrency" maxLength={3} placeholder="USD" value={form.salaryCurrency} onChange={(e) => set('salaryCurrency', e.target.value)} />
            </FormField>
          </FormSection>

          <FormSection title="Tracking" description="How much it matters and where it came from.">
            <FormField id="priority" label="Priority">
              <NativeSelect id="priority" value={form.priority} onChange={(e) => set('priority', e.target.value as Priority | '')}>
                <option value="">Not set</option>
                {PRIORITIES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id="deadline" label="Deadline">
              <Input id="deadline" type="date" value={form.deadline} onChange={(e) => set('deadline', e.target.value)} />
            </FormField>
            <FormField id="source" label="Source">
              <Input id="source" placeholder="LinkedIn, referral, careers page" value={form.source} onChange={(e) => set('source', e.target.value)} />
            </FormField>
            <CheckboxField className="self-end" checked={form.isReferral} onChange={(checked) => set('isReferral', checked)} label="I was referred" />
          </FormSection>

          <FormSection title="Recruiter" description="Who to follow up with.">
            <FormField id="recruiterName" label="Recruiter name">
              <Input id="recruiterName" value={form.recruiterName} onChange={(e) => set('recruiterName', e.target.value)} />
            </FormField>
            <FormField id="recruiterEmail" label="Recruiter email" error={fieldErrors.recruiterEmail}>
              <Input id="recruiterEmail" type="email" value={form.recruiterEmail} onChange={(e) => set('recruiterEmail', e.target.value)} />
            </FormField>
          </FormSection>

          <FormSection title="Notes and documents" description="Anything worth remembering before the next round." columns={1}>
            <FormField id="jobDescription" label="Job description">
              <Textarea id="jobDescription" value={form.jobDescription} onChange={(e) => set('jobDescription', e.target.value)} rows={4} />
            </FormField>
            <FormField id="notes" label="Notes">
              <Textarea id="notes" value={form.notes} onChange={(e) => set('notes', e.target.value)} rows={3} />
            </FormField>
            <FormField id="coverLetter" label="Cover letter">
              <Textarea id="coverLetter" value={form.coverLetter} onChange={(e) => set('coverLetter', e.target.value)} rows={4} />
            </FormField>
          </FormSection>

          {error && <FormError>{error}</FormError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create application'}
            </Button>
          </FormActions>
        </form>
      </SectionCard>
    </div>
  )
}
