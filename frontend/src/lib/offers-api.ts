import { apiFetch } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'

export type OfferStatus = 'pending' | 'negotiating' | 'accepted' | 'declined'
export type OfferWorkMode = 'onsite' | 'hybrid' | 'remote'

export const OFFER_STATUSES: OfferStatus[] = ['pending', 'negotiating', 'accepted', 'declined']
export const OFFER_WORK_MODES: OfferWorkMode[] = ['onsite', 'hybrid', 'remote']

export interface Offer {
  id: string
  application_id: string
  company_name: string
  job_title: string
  base_salary: number
  bonus: number | null
  equity_value: number | null
  signing_bonus: number | null
  first_year_total: number
  currency: string
  location: string | null
  work_mode: OfferWorkMode | null
  benefits: string | null
  notes: string | null
  start_date: string | null
  response_deadline: string | null
  days_remaining: number | null
  status: OfferStatus
  created_at: string
  updated_at: string
}

export interface OfferInput {
  application_id?: string
  base_salary?: number
  bonus?: number
  equity_value?: number
  signing_bonus?: number
  currency?: string
  location?: string
  work_mode?: OfferWorkMode
  benefits?: string
  notes?: string
  start_date?: string
  response_deadline?: string
  status?: OfferStatus
}

export const offerKeys = { all: ['offers'] as const }

export function listOffers() {
  return apiFetch<Offer[]>('/offers')
}

export function createOffer(input: OfferInput) {
  return apiFetch<Offer>('/offers', { method: 'POST', body: input })
}

export function updateOffer(id: string, input: OfferInput) {
  return apiFetch<Offer>(`/offers/${id}`, { method: 'PATCH', body: input })
}

export function deleteOffer(id: string) {
  return apiFetch<void>(`/offers/${id}`, { method: 'DELETE' })
}

export function countdownLabel(offer: Pick<Offer, 'days_remaining' | 'status'>) {
  if (offer.days_remaining === null) {
    return offer.status === 'accepted' || offer.status === 'declined' ? formatLabel(offer.status) : 'No deadline'
  }
  if (offer.days_remaining < 0) {
    return `Deadline passed ${-offer.days_remaining} ${offer.days_remaining === -1 ? 'day' : 'days'} ago`
  }
  if (offer.days_remaining === 0) {
    return 'Reply today'
  }
  return `${offer.days_remaining} ${offer.days_remaining === 1 ? 'day' : 'days'} left`
}

export type OfferFactor = 'pay' | 'growth' | 'learning' | 'work_life_balance' | 'location' | 'brand'

export const OFFER_FACTORS: { key: OfferFactor; label: string }[] = [
  { key: 'pay', label: 'Pay' },
  { key: 'growth', label: 'Growth' },
  { key: 'learning', label: 'Learning' },
  { key: 'work_life_balance', label: 'Work-life balance' },
  { key: 'location', label: 'Location' },
  { key: 'brand', label: 'Brand' },
]

export type FactorValues = Partial<Record<OfferFactor, number>>

export interface OfferDecisionEntry {
  offer_id: string
  company_name: string
  job_title: string
  ratings: FactorValues
  score: number | null
  rank: number | null
}

export interface OfferDecision {
  weights: FactorValues
  offers: OfferDecisionEntry[]
  leader_summary: string | null
}

export const offerDecisionKey = (offerIds: string[]) => ['offers', 'decision', ...offerIds] as const

export function getOfferDecision(offerIds: string[]) {
  return apiFetch<OfferDecision>(`/offers/decision?offer_ids=${offerIds.join(',')}`)
}

export function saveOfferWeights(weights: FactorValues) {
  return apiFetch<OfferDecision>('/offers/decision/weights', { method: 'PUT', body: weights })
}

export function saveOfferRatings(offerId: string, ratings: FactorValues) {
  return apiFetch<OfferDecision>(`/offers/${offerId}/ratings`, { method: 'PUT', body: ratings })
}
