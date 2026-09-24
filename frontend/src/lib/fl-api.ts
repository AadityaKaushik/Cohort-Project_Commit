import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.dto.FlSchemaResponse.FeatureBound (also fl-service's GET /fl/schema). */
export interface FlFeatureBound {
  name: string
  lower_bound: number
  upper_bound: number
}

/** Mirrors com.cohort.api.dto.FlSchemaResponse (US-57/58). */
export interface FlSchema {
  schema_version: number
  feature_count: number
  features: FlFeatureBound[]
  not_collected: string[]
}

/** Mirrors com.cohort.api.dto.FlConsentResponse (US-57/US-64). */
export interface FlConsent {
  opted_in: boolean
}

export function getFlSchema() {
  return apiFetch<FlSchema>('/fl/schema')
}

export function getFlConsent() {
  return apiFetch<FlConsent>('/fl/consent')
}

export function optInFl() {
  return apiFetch<FlConsent>('/fl/opt-in', { method: 'POST' })
}

export function optOutFl() {
  return apiFetch<FlConsent>('/fl/opt-out', { method: 'POST' })
}

/** Mirrors com.cohort.api.dto.PrivacyLedgerEntryResponse (US-63): one immutable row per round this client took part in. */
export interface PrivacyLedgerEntry {
  id: string
  round_number: number
  schema_version: number
  pre_clip_norm: number
  post_clip_norm: number
  noise_multiplier: number
  cohort_size: number
  epsilon: number
  /** The real post-clip, post-noise vector fl-service transmitted, never a placeholder. */
  transmitted_vector: number[]
  created_at: string
}

export function listPrivacyLedger() {
  return apiFetch<PrivacyLedgerEntry[]>('/fl/privacy-ledger')
}
