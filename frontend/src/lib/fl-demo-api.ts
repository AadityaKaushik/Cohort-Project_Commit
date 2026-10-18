import { apiFetch } from '@/lib/api-client'

export interface FlWeight {
  feature: string
  value: number
}

export interface FlDemoRound {
  round_number: number
  created_at: string
  accepted: boolean
  reason: string | null
  cohort_size: number
  sat_out_count: number
  model_version: number | null
  check_accuracy: number | null
  noise_multiplier: number | null
}

export interface FlDemoStatus {
  available: boolean
  unavailable_reason: string | null
  opted_in: boolean
  your_rated_rounds: number
  minimum_rated_rounds: number
  your_predicted_confidence: number | null
  synthetic_clients: number
  noise_levels: number[]
  central_ceiling: number | null
  model: {
    version: number
    round_number: number
    participant_count: number
    check_accuracy: number | null
    weights: FlWeight[]
    intercept: number
  } | null
  versions: { version: number; round_number: number; participant_count: number; check_accuracy: number | null }[]
  rounds: FlDemoRound[]
}

export interface FlDemoRoundResult {
  exported_users: number
  export_failures: number
  round_ran: boolean
  round_number: number | null
  accepted: boolean | null
  reason: string | null
  cohort_size: number | null
  sat_out_count: number | null
  model_version: number | null
  check_accuracy: number | null
}

export function getFlDemoStatus() {
  return apiFetch<FlDemoStatus>('/dev/fl/status')
}

export function setSyntheticClients(count: number, seed = 7) {
  return apiFetch<void>('/dev/fl/synthetic-clients', { method: 'PUT', body: { count, seed } })
}

export function runFlDemoRound(noiseMultiplier: number) {
  return apiFetch<FlDemoRoundResult>('/dev/fl/round', { method: 'POST', body: { noise_multiplier: noiseMultiplier } })
}

export function resetFlDemo() {
  return apiFetch<void>('/dev/fl/reset', { method: 'POST' })
}

export function featureLabel(name: string): string {
  return name.replace(/^share_/, 'share of ').replace(/_/g, ' ').replace(/dsa/, 'DSA')
}
