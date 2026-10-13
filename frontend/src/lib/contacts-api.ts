import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.entity.Contact.Relationship. */
export type ContactRelationship = 'recruiter' | 'hiring_manager' | 'referrer' | 'peer' | 'alumni' | 'other'

/** Mirrors com.cohort.api.entity.Contact.ReferralStatus. */
export type ReferralStatus = 'none' | 'requested' | 'agreed' | 'submitted' | 'declined'

export const RELATIONSHIPS: ContactRelationship[] = ['recruiter', 'hiring_manager', 'referrer', 'peer', 'alumni', 'other']
export const REFERRAL_STATUSES: ReferralStatus[] = ['none', 'requested', 'agreed', 'submitted', 'declined']

/** Mirrors com.cohort.api.dto.ContactResponse (US-95). */
export interface Contact {
  id: string
  name: string
  role_title: string | null
  email: string | null
  linkedin_url: string | null
  relationship: ContactRelationship
  referral_status: ReferralStatus
  company_id: string | null
  company_name: string | null
  application_id: string | null
  notes: string | null
  last_contacted_at: string | null
  cold: boolean
  created_at: string
  updated_at: string
}

export interface ContactInput {
  name?: string
  role_title?: string
  email?: string
  linkedin_url?: string
  relationship?: ContactRelationship
  referral_status?: ReferralStatus
  company_id?: string
  application_id?: string
  notes?: string
}

export const contactKeys = {
  all: ['contacts'] as const,
  list: (referralStatus: ReferralStatus | 'all') => ['contacts', 'list', referralStatus] as const,
}

export function listContacts(referralStatus?: ReferralStatus) {
  const query = referralStatus ? `?referral_status=${referralStatus}` : ''
  return apiFetch<Contact[]>(`/contacts${query}`)
}

export function createContact(input: ContactInput) {
  return apiFetch<Contact>('/contacts', { method: 'POST', body: input })
}

export function updateContact(id: string, input: ContactInput) {
  return apiFetch<Contact>(`/contacts/${id}`, { method: 'PATCH', body: input })
}

export function markContactContacted(id: string) {
  return apiFetch<Contact>(`/contacts/${id}/contacted`, { method: 'POST' })
}

export function deleteContact(id: string) {
  return apiFetch<void>(`/contacts/${id}`, { method: 'DELETE' })
}
