import { apiFetch } from '@/lib/api-client'

export interface DirectorySource {
  url: string
  title: string
}

export interface DirectoryRound {
  name: string
  format: string | null
  tests: string | null
  duration: string | null
}

export interface DirectoryPay {
  min: number
  max: number
  currency: string
  unit: string
  year: number | null
  level: string
  note: string | null
}

/** Mirrors com.cohort.api.dto.DirectoryCompanyListItem (US-96). */
export interface DirectoryCompanyListItem {
  slug: string
  name: string
  industry: string | null
  website: string | null
  role_families: string[]
  difficulty_level: number | null
  difficulty_basis: string | null
  last_checked: string
}

export interface DirectoryRolePattern {
  role_family: string
  level: string
  rounds: DirectoryRound[]
  topic_slugs: string[]
  notes: string | null
  experience_summary: string | null
  pay: DirectoryPay | null
  difficulty_level: number | null
  difficulty_basis: string | null
  sources: DirectorySource[]
  last_checked: string
}

/** Mirrors com.cohort.api.dto.DirectoryCompanyDetail. */
export interface DirectoryCompanyDetail {
  slug: string
  name: string
  industry: string | null
  locations: string | null
  size_band: string | null
  website: string | null
  summary: string | null
  sources: DirectorySource[]
  last_checked: string
  community_rating: number | null
  community_min_contributors: number
  role_patterns: DirectoryRolePattern[]
}

export const directoryKeys = {
  list: (query: string) => ['directory', 'list', query] as const,
  detail: (slug: string) => ['directory', 'detail', slug] as const,
}

export function listDirectoryCompanies(query = '') {
  const params = query.trim() ? `?query=${encodeURIComponent(query.trim())}` : ''
  return apiFetch<DirectoryCompanyListItem[]>(`/directory/companies${params}`)
}

export function getDirectoryCompany(slug: string) {
  return apiFetch<DirectoryCompanyDetail>(`/directory/companies/${encodeURIComponent(slug)}`)
}
