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

/** Mirrors com.cohort.api.dto.RecommendationResponse (US-51..56, US-65). */
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
  /** US-65: whether a federated model signal was actually folded into this ranking (P53's circuit breaker falls back to false, never an error). */
  federated_applied: boolean
  /** US-65: position change vs. the local-only order; always 0 when federated_applied is false. */
  rank_delta: number
}

/** {@code federated}: US-65's Federated Lift toggle -- true re-ranks by the current global model (bounded to a ±25% nudge per candidate), false is always the local-only ranking. */
export function listRecommendations(federated = false) {
  return apiFetch<Recommendation[]>(`/recommendations?federated=${federated}`)
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
