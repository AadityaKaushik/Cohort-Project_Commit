import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.entity.ApplicationStatus (PROJECT_PLAN.md §17.5). */
export type ApplicationStatusValue =
  | 'SAVED'
  | 'APPLIED'
  | 'OA'
  | 'INTERVIEW'
  | 'FINAL_ROUND'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'ACCEPTED'
  | 'DECLINED'

export type WorkMode = 'onsite' | 'hybrid' | 'remote'
export type JobType = 'internship' | 'full_time' | 'part_time' | 'contract'
export type Priority = 'low' | 'medium' | 'high'

export interface CompanySummary {
  id: string
  name: string
  website: string | null
  industry: string | null
}

export interface ApplicationSummary {
  id: string
  company: CompanySummary
  job_title: string
  status: ApplicationStatusValue
  current_stage: ApplicationStatusValue
  days_in_stage: number
  date_applied: string
  priority: Priority | null
  archived: boolean
}

export interface ApplicationStatusHistoryEntry {
  id: string
  from_status: ApplicationStatusValue | null
  to_status: ApplicationStatusValue
  note: string | null
  changed_at: string
}

/** Mirrors com.cohort.api.dto.ApplicationInterviewSummary — the small slice embedded in ApplicationDetail (FR-17). */
export interface ApplicationInterviewEntry {
  id: string
  round_number: number
  round_type: string
  scheduled_at: string | null
  completed_at: string | null
  outcome: 'passed' | 'failed' | 'pending' | 'unknown'
}

export interface ApplicationDetail {
  id: string
  company: CompanySummary
  resume_id: string | null
  job_title: string
  job_description: string | null
  job_url: string | null
  location: string | null
  work_mode: WorkMode | null
  job_type: JobType | null
  salary_min: number | null
  salary_max: number | null
  salary_currency: string | null
  date_applied: string
  status: ApplicationStatusValue
  current_stage: ApplicationStatusValue
  days_in_stage: number
  priority: Priority | null
  source: string | null
  is_referral: boolean
  deadline: string | null
  recruiter_name: string | null
  recruiter_email: string | null
  notes: string | null
  cover_letter: string | null
  archived_at: string | null
  stage_entered_at: string
  created_at: string
  updated_at: string
  history: ApplicationStatusHistoryEntry[]
  interviews: ApplicationInterviewEntry[]
}

export interface PagedResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface ApplicationFormInput {
  company_name?: string
  job_title?: string
  job_description?: string | null
  job_url?: string | null
  location?: string | null
  work_mode?: WorkMode | null
  job_type?: JobType | null
  salary_min?: number | null
  salary_max?: number | null
  salary_currency?: string | null
  date_applied?: string
  status?: ApplicationStatusValue
  priority?: Priority | null
  source?: string | null
  is_referral?: boolean
  deadline?: string | null
  recruiter_name?: string | null
  recruiter_email?: string | null
  notes?: string | null
  cover_letter?: string | null
}

export interface ListApplicationsParams {
  page?: number
  size?: number
  sort?: 'date_applied' | 'status' | 'priority' | 'days_in_stage'
  direction?: 'asc' | 'desc'
}

export function listApplications(params: ListApplicationsParams = {}) {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.sort) query.set('sort', params.sort)
  if (params.direction) query.set('direction', params.direction)
  const queryString = query.toString()
  return apiFetch<PagedResponse<ApplicationSummary>>(`/applications${queryString ? `?${queryString}` : ''}`)
}

export function getApplication(id: string) {
  return apiFetch<ApplicationDetail>(`/applications/${id}`)
}

export function createApplication(input: ApplicationFormInput) {
  return apiFetch<ApplicationDetail>('/applications', { method: 'POST', body: input })
}

export function updateApplication(id: string, input: ApplicationFormInput) {
  return apiFetch<ApplicationDetail>(`/applications/${id}`, { method: 'PATCH', body: input })
}

export function deleteApplication(id: string) {
  return apiFetch<void>(`/applications/${id}`, { method: 'DELETE' })
}

export function changeApplicationStatus(id: string, input: { to_status: ApplicationStatusValue; note?: string }) {
  return apiFetch<ApplicationDetail>(`/applications/${id}/status`, { method: 'POST', body: input })
}

export function getApplicationHistory(id: string) {
  return apiFetch<ApplicationStatusHistoryEntry[]>(`/applications/${id}/history`)
}

/** POST /applications/{id}/resume (US-24): `resumeId: null` clears the association. */
export function attachResumeToApplication(id: string, resumeId: string | null) {
  return apiFetch<ApplicationDetail>(`/applications/${id}/resume`, {
    method: 'POST',
    body: { resume_id: resumeId },
  })
}
