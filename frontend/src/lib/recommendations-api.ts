import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.service.recommendation.RecommendationType. */
export type RecommendationType = 'weak_topic' | 'prerequisite_gap' | 'decay' | 'stalled_application' | 'baseline'

/** Mirrors com.cohort.api.service.recommendation.tiering.EvidenceTier. */
export type EvidenceTier = 'A' | 'B' | 'C' | 'D'

/** Mirrors com.cohort.api.service.recommendation.RecommendationStatus. */
export type RecommendationStatus = 'active' | 'accepted' | 'snoozed' | 'dismissed'

/** Mirrors com.cohort.api.dto.EvidenceItemResponse. */
export interface EvidenceItem {
  label: string
  detail: string | null
  occurred_at: string | null
}

/** Mirrors com.cohort.api.dto.RecommendationResponse (US-51..56). */
export interface Recommendation {
  id: string
  type: RecommendationType
  tier: EvidenceTier
  target_topic_id: string | null
  target_application_id: string | null
  score: number
  headline: string
  explanation: string
  observation_count: number
  evidence: EvidenceItem[]
  status: RecommendationStatus
  snoozed_until: string | null
  dismissed_reason: string | null
  accepted_task_id: string | null
  generated_at: string
}

export function listRecommendations() {
  return apiFetch<Recommendation[]>('/recommendations')
}

export function acceptRecommendation(id: string) {
  return apiFetch<Recommendation>(`/recommendations/${id}/accept`, { method: 'POST' })
}

export function snoozeRecommendation(id: string) {
  return apiFetch<Recommendation>(`/recommendations/${id}/snooze`, { method: 'POST' })
}

export function dismissRecommendation(id: string, reason: string) {
  return apiFetch<Recommendation>(`/recommendations/${id}/dismiss`, { method: 'POST', body: { reason } })
}
