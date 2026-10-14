import { apiFetch } from '@/lib/api-client'

export type RecommendationType = 'weak_topic' | 'prerequisite_gap' | 'decay' | 'stalled_application' | 'baseline'

export type EvidenceTier = 'A' | 'B' | 'C' | 'D'

export type RecommendationStatus = 'active' | 'accepted' | 'snoozed' | 'dismissed'

export interface EvidenceItem {
  label: string
  detail: string | null
  occurred_at: string | null
}

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
  // US-65: whether a federated model signal was actually folded into this ranking (P53's circuit breaker falls back to false, never an error).
  federated_applied: boolean
  rank_delta: number
  suppressed_until: string | null
  handled_at: string | null
}

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

export function listHandledRecommendations() {
  return apiFetch<Recommendation[]>('/recommendations/handled')
}

export function restoreRecommendation(id: string) {
  return apiFetch<Recommendation>(`/recommendations/${id}/restore`, { method: 'POST' })
}
