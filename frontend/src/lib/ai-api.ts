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
