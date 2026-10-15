import { apiFetch } from '@/lib/api-client'

export interface SampleDataStatus {
  loaded: boolean
  applications: number
  interviews: number
  contacts: number
}

export const sampleDataKey = ['sample-data'] as const

export function getSampleData() {
  return apiFetch<SampleDataStatus>('/sample-data')
}

export function loadSampleData() {
  return apiFetch<SampleDataStatus>('/sample-data', { method: 'POST' })
}

export function removeSampleData() {
  return apiFetch<SampleDataStatus>('/sample-data', { method: 'DELETE' })
}
