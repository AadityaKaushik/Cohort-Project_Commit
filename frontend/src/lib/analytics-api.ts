import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.dto.FunnelResponse (US-43). Every count is "ever reached," from history, not current status. */
export interface Funnel {
  applied: number
  reached_oa: number
  reached_interview: number
  reached_final: number
  reached_offer: number
  sample_size: number
}

/** Mirrors com.cohort.api.dto.ConversionRateView (US-44, FR-67). */
export interface ConversionRate {
  value: number
  formatted: string
  numerator: number
  denominator: number
  sufficient_data: boolean
}

/** Mirrors com.cohort.api.dto.ConversionRatesResponse. */
export interface ConversionRates {
  application_to_oa: ConversionRate
  oa_to_interview: ConversionRate
  interview_to_final: ConversionRate
  final_to_offer: ConversionRate
  overall_success: ConversionRate
}

/** Mirrors com.cohort.api.dto.TimelineResponse (US-45). */
export interface TimelineBucket {
  week_start: string
  applications: number
  interviews: number
}

export interface Timeline {
  buckets: TimelineBucket[]
  sample_size: number
}

/** Mirrors com.cohort.api.entity.ApplicationStatus's non-terminal, pipeline-ordered members plus the four terminal outcomes. */
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

/** Mirrors com.cohort.api.dto.RejectionDistributionResponse (US-46). */
export interface RejectionDistribution {
  by_stage: Partial<Record<ApplicationStatusValue, number>>
  no_response_count: number
  sample_size: number
  sufficient_data: boolean
}

/** Mirrors com.cohort.api.dto.DwellTimesResponse.DwellView (US-48). */
export interface DwellView {
  median_days: number
  p25_days: number
  p75_days: number
  completed_count: number
  censored_count: number
  sufficient_data: boolean
}

export interface DwellTimes {
  by_stage: Partial<Record<ApplicationStatusValue, DwellView>>
}

export function getFunnel() {
  return apiFetch<Funnel>('/analytics/funnel')
}

export function getConversionRates() {
  return apiFetch<ConversionRates>('/analytics/conversions')
}

export interface TimelineParams {
  from?: string
  to?: string
}

export function getTimeline(params: TimelineParams = {}) {
  const query = new URLSearchParams()
  if (params.from) query.set('from', params.from)
  if (params.to) query.set('to', params.to)
  const queryString = query.toString()
  return apiFetch<Timeline>(`/analytics/timeline${queryString ? `?${queryString}` : ''}`)
}

export function getRejectionDistribution() {
  return apiFetch<RejectionDistribution>('/analytics/rejections')
}

export function getDwellTimes() {
  return apiFetch<DwellTimes>('/analytics/dwell-times')
}
