import { apiFetch } from '@/lib/api-client'
import type { ApplicationSummary } from '@/lib/applications-api'
import type { Contact } from '@/lib/contacts-api'

/** Mirrors com.cohort.api.dto.CompanyListItem (US-96). */
export interface CompanyListItem {
  id: string
  name: string
  website: string | null
  industry: string | null
  application_count: number
  active_application_count: number
  contact_count: number
  last_activity_at: string | null
}

export interface CompanyNote {
  id: string
  body: string
  created_at: string
  updated_at: string
}

/** Mirrors com.cohort.api.dto.CompanyDetailResponse. */
export interface CompanyDetail {
  id: string
  name: string
  website: string | null
  industry: string | null
  stats: {
    applications: number
    active: number
    interviews: number
    offers: number
    last_activity_at: string | null
  }
  applications: ApplicationSummary[]
  contacts: Contact[]
  notes: CompanyNote[]
}

export const companyKeys = {
  all: ['companies'] as const,
  list: (query: string) => ['companies', 'list', query] as const,
  detail: (id: string) => ['companies', 'detail', id] as const,
}

export function listCompanies(query = '') {
  const params = query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''
  return apiFetch<CompanyListItem[]>(`/companies${params}`)
}

export function getCompany(id: string) {
  return apiFetch<CompanyDetail>(`/companies/${id}`)
}

export function updateCompany(id: string, input: { website?: string; industry?: string }) {
  return apiFetch<CompanyDetail>(`/companies/${id}`, { method: 'PATCH', body: input })
}

export function addCompanyNote(companyId: string, body: string) {
  return apiFetch<CompanyNote>(`/companies/${companyId}/notes`, { method: 'POST', body: { body } })
}

export function deleteCompanyNote(companyId: string, noteId: string) {
  return apiFetch<void>(`/companies/${companyId}/notes/${noteId}`, { method: 'DELETE' })
}
