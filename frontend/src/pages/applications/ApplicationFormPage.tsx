import { useEffect, useRef, useState, type ClipboardEvent, type FormEvent } from 'react'
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
import { parsePosting, type PostingField } from '@/lib/posting-parser'
import { readBookmarkParams } from '@/lib/bookmarklet'
import { SaveFromSitesCard } from '@/components/applications/SaveFromSitesCard'
import { listCompanies } from '@/lib/companies-api'
import { formatLabel } from '@/lib/labels'
import { cleanName, email as emailRule, hasErrors, meaningfulText, plausibleDate, positiveInteger, required, url, validate } from '@/lib/validation'
import { useToast } from '@/hooks/use-toast'
import { useUnsavedChanges } from '@/components/shared/UnsavedChangesGuard'
import { useSoftWarnings } from '@/hooks/use-soft-warnings'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { CheckboxField, FormActions, FormError, FormField, FormSection } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { ErrorState, PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { SoftWarning } from '@/components/shared/SoftWarning'

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
  const [searchParams, setSearchParams] = useSearchParams()
  const bookmarkRead = useRef(false)
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

  const today = format(new Date(), 'yyyy-MM-dd')
  const [form, setForm] = useState<FormState>(() => {
    const start = { ...EMPTY_FORM, dateApplied: today }
    return start
  })
  const [baseline, setBaseline] = useState<FormState>({ ...EMPTY_FORM, dateApplied: today })
  const dirty = JSON.stringify(form) !== JSON.stringify(baseline)
  const { guard, markSaved } = useUnsavedChanges(dirty)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const softWarnings = useSoftWarnings()

  const [bookmarkNote, setBookmarkNote] = useState<string | null>(null)
  const [postingOpen, setPostingOpen] = useState(false)
  const [postingText, setPostingText] = useState('')
  const [postingUrl, setPostingUrl] = useState('')
  const [suggested, setSuggested] = useState<PostingField[]>([])
  const [postingSummary, setPostingSummary] = useState<string | null>(null)

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
    setSuggested((current) => current.filter((field) => field !== key))
  }

  function suggestedHint(field: PostingField) {
    return suggested.includes(field) ? 'Suggested from the posting. Edit if it is wrong.' : undefined
  }

  function fillFromPosting() {
    const parsed = parsePosting(postingText, postingUrl)
    const filled = (Object.keys(parsed) as PostingField[]).filter((field) => form[field] === '')
    if (filled.length === 0) {
      setPostingSummary(
        Object.keys(parsed).length === 0 ? 'Nothing could be read from that text.' : 'Every field it found is already filled.',
      )
      return
    }
    setForm((current) => {
      const next = { ...current }
      for (const field of filled) {
        next[field] = parsed[field] as never
      }
      return next
    })
    setSuggested((current) => [...new Set([...current, ...filled])])
    setPostingSummary(`Filled ${filled.length} ${filled.length === 1 ? 'field' : 'fields'}. Review before saving.`)
  }

  useEffect(() => {
    if (bookmarkRead.current || isEditing) return
    bookmarkRead.current = true
    const bookmark = readBookmarkParams(searchParams.toString())
    if (!bookmark) return
    const parsed = parsePosting(bookmark.text, bookmark.url)
    const wanted: Record<string, string | undefined> = {
      companyName: bookmark.company || parsed.companyName,
      jobTitle: bookmark.title || parsed.jobTitle,
      location: bookmark.location || parsed.location,
      jobUrl: bookmark.url || parsed.jobUrl,
      workMode: parsed.workMode,
      jobType: parsed.jobType,
      jobDescription: parsed.jobDescription,
      salaryMin: parsed.salaryMin,
      salaryMax: parsed.salaryMax,
      salaryCurrency: parsed.salaryCurrency,
    }
    setForm((current) => {
      const next = { ...current } as Record<string, unknown>
      for (const [field, value] of Object.entries(wanted)) {
        if (value && next[field] === '') next[field] = value
      }
      return next as unknown as FormState
    })
    setBookmarkNote('Filled from your bookmark. Review before saving.')
    setSearchParams({}, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs once on load
  }, [isEditing, setSearchParams])

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
        deadline: form.deadline,
        location: form.location,
        source: form.source,
        recruiterName: form.recruiterName,
        notes: form.notes,
        jobUrl: form.jobUrl,
        salaryMin: form.salaryMin,
        salaryMax: form.salaryMax,
        salaryCurrency: form.salaryCurrency,
        recruiterEmail: form.recruiterEmail,
      },
      {
        companyName: [required('Company'), cleanName('Company')],
        jobTitle: [required('Job title'), cleanName('Job title')],
        dateApplied: [required('Date applied'), plausibleDate('Date applied')],
        deadline: [plausibleDate('Deadline')],
        location: [cleanName('Location')],
        source: [cleanName('Source')],
        recruiterName: [cleanName('Recruiter name')],
        notes: [meaningfulText],
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
    if ((form.salaryMin || form.salaryMax) && form.salaryCurrency === '' && !problems.salaryCurrency) {
      problems.salaryCurrency = 'Add the currency for this salary, like INR or USD.'
    }
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    if (softWarnings.check('notes', form.notes, false).length > 0) {
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
        actions={<PageGuide guideId="application-form" />}
      />
      {bookmarkNote && (
        <p role="status" className="rounded-lg border border-border bg-surface-2 px-4 py-3 text-sm">
          {bookmarkNote}
        </p>
      )}
      <SectionCard>
        <div data-guide="posting" className="mb-6 rounded-lg border border-border p-4">
          <button
            type="button"
            className="flex w-full items-center justify-between text-left text-sm font-medium"
            aria-expanded={postingOpen}
            aria-controls="posting-panel"
            onClick={() => setPostingOpen((open) => !open)}
          >
            Paste a job posting
            <span aria-hidden className="text-muted-foreground">
              {postingOpen ? '−' : '+'}
            </span>
          </button>
          {postingOpen && (
            <div id="posting-panel" className="mt-4 space-y-4">
              <FormField id="postingText" label="Posting text" hint="Empty fields are filled from this text. Fields you already typed stay as they are.">
                <Textarea id="postingText" rows={6} value={postingText} onChange={(e) => setPostingText(e.target.value)} />
              </FormField>
              <FormField id="postingUrl" label="Posting link (optional)">
                <Input id="postingUrl" type="url" placeholder="https://" value={postingUrl} onChange={(e) => setPostingUrl(e.target.value)} />
              </FormField>
              <div className="flex items-center gap-3">
                <Button type="button" variant="outline" onClick={fillFromPosting} disabled={postingText.trim() === ''}>
                  Fill the form
                </Button>
                {postingSummary && (
                  <p role="status" className="text-sm text-muted-foreground">
                    {postingSummary}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
        <form id="application-form" data-guide="form" onSubmit={handleSubmit} className="space-y-6" noValidate>
          <FormSection title="The role" description="What you applied for and where it stands.">
            <FormField id="companyName" label="Company" hint={suggestedHint('companyName')} error={fieldErrors.companyName}>
              <Input id="companyName" list="company-options" autoComplete="off" value={form.companyName} onChange={(e) => set('companyName', e.target.value)} required />
            </FormField>
            <datalist id="company-options">
              {companiesQuery.data?.map((company) => (
                <option key={company.id} value={company.name} />
              ))}
            </datalist>
            <FormField id="jobTitle" label="Job title" hint={suggestedHint('jobTitle')} error={fieldErrors.jobTitle}>
              <Input id="jobTitle" value={form.jobTitle} onChange={(e) => set('jobTitle', e.target.value)} required />
            </FormField>
            <FormField id="dateApplied" label="Date applied" error={fieldErrors.dateApplied}>
              <Input id="dateApplied" type="date" value={form.dateApplied} onChange={(e) => set('dateApplied', e.target.value)} required />
            </FormField>
            {!isEditing && (
              <FormField id="status" label="Status" error={fieldErrors.status}>
                <NativeSelect id="status" value={form.status} onChange={(e) => set('status', e.target.value as ApplicationStatusValue)}>
                  {CREATE_STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {formatLabel(option)}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
            )}
            <FormField id="location" label="Location" hint={suggestedHint('location')} error={fieldErrors.location}>
              <Input id="location" value={form.location} onChange={(e) => set('location', e.target.value)} />
            </FormField>
            <FormField id="workMode" label="Work mode" hint={suggestedHint('workMode')}>
              <NativeSelect id="workMode" value={form.workMode} onChange={(e) => set('workMode', e.target.value as WorkMode | '')}>
                <option value="">Not specified</option>
                {WORK_MODES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id="jobType" label="Job type" hint={suggestedHint('jobType')}>
              <NativeSelect id="jobType" value={form.jobType} onChange={(e) => set('jobType', e.target.value as JobType | '')}>
                <option value="">Not specified</option>
                {JOB_TYPES.map((option) => (
                  <option key={option} value={option}>
                    {formatLabel(option)}
                  </option>
                ))}
              </NativeSelect>
            </FormField>
            <FormField id="jobUrl" label="Job URL" hint={suggestedHint('jobUrl')} error={fieldErrors.jobUrl}>
              <Input id="jobUrl" type="url" placeholder="https://" value={form.jobUrl} onPaste={handleUrlPaste} onChange={(e) => set('jobUrl', e.target.value)} />
            </FormField>
          </FormSection>

          <FormSection title="Compensation" description="Whole numbers, in the posting's currency." columns={3}>
            <FormField id="salaryMin" label="Minimum salary" hint={suggestedHint('salaryMin')} error={fieldErrors.salaryMin}>
              <Input id="salaryMin" inputMode="numeric" value={form.salaryMin} onChange={(e) => set('salaryMin', e.target.value)} />
            </FormField>
            <FormField id="salaryMax" label="Maximum salary" hint={suggestedHint('salaryMax')} error={fieldErrors.salaryMax}>
              <Input id="salaryMax" inputMode="numeric" value={form.salaryMax} onChange={(e) => set('salaryMax', e.target.value)} />
            </FormField>
            <FormField id="salaryCurrency" label="Currency" hint={suggestedHint('salaryCurrency')} error={fieldErrors.salaryCurrency}>
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
            <FormField id="deadline" label="Deadline" error={fieldErrors.deadline}>
              <Input id="deadline" type="date" value={form.deadline} onChange={(e) => set('deadline', e.target.value)} />
            </FormField>
            <FormField id="source" label="Source" error={fieldErrors.source}>
              <Input id="source" placeholder="LinkedIn, referral, careers page" value={form.source} onChange={(e) => set('source', e.target.value)} />
            </FormField>
            <CheckboxField className="self-end" checked={form.isReferral} onChange={(checked) => set('isReferral', checked)} label="I was referred" />
          </FormSection>

          <FormSection title="Recruiter" description="Who to follow up with.">
            <FormField id="recruiterName" label="Recruiter name" error={fieldErrors.recruiterName}>
              <Input id="recruiterName" value={form.recruiterName} onChange={(e) => set('recruiterName', e.target.value)} />
            </FormField>
            <FormField id="recruiterEmail" label="Recruiter email" error={fieldErrors.recruiterEmail}>
              <Input id="recruiterEmail" type="email" value={form.recruiterEmail} onChange={(e) => set('recruiterEmail', e.target.value)} />
            </FormField>
          </FormSection>

          <FormSection title="Notes and documents" description="Anything worth remembering before the next round." columns={1}>
            <FormField id="jobDescription" label="Job description" hint={suggestedHint('jobDescription')}>
              <Textarea id="jobDescription" value={form.jobDescription} onChange={(e) => set('jobDescription', e.target.value)} rows={4} />
            </FormField>
            <FormField id="notes" label="Notes" error={fieldErrors.notes}>
              <Textarea
                id="notes"
                value={form.notes}
                onChange={(e) => set('notes', e.target.value)}
                onBlur={() => softWarnings.check('notes', form.notes, false)}
                rows={3}
              />
            </FormField>
            <SoftWarning
              id="notes"
              messages={softWarnings.shown.notes}
              onEdit={() => {
                softWarnings.dismiss('notes')
                document.getElementById('notes')?.focus()
              }}
              onSaveAnyway={() => {
                softWarnings.accept('notes', form.notes)
                document.querySelector<HTMLFormElement>('#application-form')?.requestSubmit()
              }}
            />
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
      {!isEditing && !duplicateId && (
        <div data-guide="save-from-sites">
          <SaveFromSitesCard />
        </div>
      )}
    </div>
  )
}
