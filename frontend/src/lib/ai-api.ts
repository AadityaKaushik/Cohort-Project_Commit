import { apiFetch } from '@/lib/api-client'

export interface AiStatus {
  provider: string
  hosted: boolean
}

export const aiKeys = {
  status: ['ai', 'status'] as const,
  match: (applicationId: string) => ['ai', 'match', applicationId] as const,
  prepPack: (interviewId: string) => ['ai', 'prep-pack', interviewId] as const,
}

export function getAiStatus() {
  return apiFetch<AiStatus>('/ai/status')
}

export interface TailoringBullet {
  text: string
  keywords: string[]
  source: string
}

export interface ResumeMatch {
  has_job_description: boolean
  has_resume: boolean
  required: string[]
  matched: string[]
  missing: string[]
  score: number | null
  explanation: string
  bullets: TailoringBullet[]
}

export interface AiDraft {
  draft: string
  source: 'model' | 'template'
  ai_available: boolean
}

export function getResumeMatch(applicationId: string) {
  return apiFetch<ResumeMatch>(`/applications/${applicationId}/match`)
}

export function createCoverLetterDraft(applicationId: string) {
  return apiFetch<AiDraft>(`/applications/${applicationId}/cover-letter-draft`, { method: 'POST' })
}

export interface PrepPack {
  interview_id: string
  company_name: string
  job_title: string
  round_number: number
  round_type: string
  scheduled_at: string | null
  interviewer_name: string | null
  interviewer_role: string | null
  company_notes: string[]
  past_rounds: {
    round_number: number
    round_type: string
    job_title: string
    outcome: string
    went_well: string | null
    went_poorly: string | null
    improvement_areas: string | null
  }[]
  questions_asked: { text: string; category: string | null; round_number: number }[]
  weak_topics: { name: string; has_evidence: boolean; mastery_percent: number }[]
  resume_highlights: string[]
  readiness_score: number | null
  readiness_formatted: string
}

export function getPrepPack(interviewId: string) {
  return apiFetch<PrepPack>(`/interviews/${interviewId}/prep-pack`)
}

export function createPrepSummary(interviewId: string) {
  return apiFetch<AiDraft>(`/interviews/${interviewId}/prep-pack/summary`, { method: 'POST' })
}

export type EmailDraftKind = 'thank_you' | 'follow_up' | 'negotiation' | 'referral_request' | 'reconnect' | 'referral_thanks'
export type ContactDraftKind = 'referral_request' | 'reconnect' | 'referral_thanks'

export interface EmailDraft {
  kind: EmailDraftKind
  subject: string
  body: string
  source: 'model' | 'template'
  ai_available: boolean
}

export const EMAIL_DRAFT_LABELS: Record<EmailDraftKind, string> = {
  thank_you: 'Thank-you after an interview',
  follow_up: 'Follow-up after silence',
  negotiation: 'Offer negotiation',
  referral_request: 'Referral request',
  reconnect: 'Reconnect',
  referral_thanks: 'Thank-you after a referral',
}

export function createEmailDraft(applicationId: string, kind: EmailDraftKind, interviewId?: string) {
  return apiFetch<EmailDraft>(`/applications/${applicationId}/email-drafts`, {
    method: 'POST',
    body: { kind, interview_id: interviewId },
  })
}

export function createContactDraft(contactId: string, kind: ContactDraftKind) {
  return apiFetch<EmailDraft>(`/contacts/${contactId}/message-drafts`, {
    method: 'POST',
    body: { kind },
  })
}
