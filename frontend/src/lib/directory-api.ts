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

export interface CommunityRoundShare {
  round_type: string
  percent: number
  average_difficulty: number
}

/** Mirrors com.cohort.api.dto.CommunitySummary. Figures are null or empty below the minimum. */
export interface CommunitySummary {
  role_family: string
  contributors: number
  rating: number | null
  difficulty: number | null
  common_rounds: CommunityRoundShare[]
  outcomes: Record<string, number>
  tips: string[]
}

export const ROUND_TYPES = [
  'online_test',
  'coding',
  'technical',
  'system_design',
  'case_study',
  'group_discussion',
  'hr',
  'managerial',
] as const

export const CONTRIBUTION_ROLE_FAMILIES = [
  'software_engineer',
  'data_scientist',
  'product_manager',
  'consulting_analyst',
  'investment_banking_analyst',
] as const

export const CONTRIBUTION_OUTCOMES = ['offer', 'rejected', 'withdrawn', 'pending'] as const

export interface ContributionRound {
  round_type: string
  difficulty: number
}

export interface ContributionInput {
  company_slug?: string
  company_name?: string
  role_family: string
  level: 'fresher' | 'experienced'
  year: number
  rounds: ContributionRound[]
  overall_difficulty: number
  overall_experience: number
  outcome: string
  tip: string | null
}

export interface Contribution extends Omit<ContributionInput, 'company_slug' | 'company_name' | 'tip'> {
  id: string
  company_slug: string
  company_name: string
  tip: string | null
  created_at: string
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
  community_difficulty: number | null
  community_contributors: number
  community_min_contributors: number
  community_summaries: CommunitySummary[]
  role_patterns: DirectoryRolePattern[]
}

export const directoryKeys = {
  list: (query: string) => ['directory', 'list', query] as const,
  detail: (slug: string) => ['directory', 'detail', slug] as const,
  mine: ['directory', 'contributions', 'mine'] as const,
}

export function listDirectoryCompanies(query = '') {
  const params = query.trim() ? `?query=${encodeURIComponent(query.trim())}` : ''
  return apiFetch<DirectoryCompanyListItem[]>(`/directory/companies${params}`)
}

export function getDirectoryCompany(slug: string) {
  return apiFetch<DirectoryCompanyDetail>(`/directory/companies/${encodeURIComponent(slug)}`)
}

export function createContribution(input: ContributionInput) {
  return apiFetch<Contribution>('/directory/contributions', { method: 'POST', body: input })
}

export function listMyContributions() {
  return apiFetch<Contribution[]>('/directory/contributions/mine')
}

export function withdrawContribution(id: string) {
  return apiFetch<void>(`/directory/contributions/${id}`, { method: 'DELETE' })
}
