import { apiFetch } from '@/lib/api-client'

export interface SearchResult {
  id: string
  title: string
  subtitle: string | null
  parent_id: string | null
}

export interface SearchResponse {
  applications: SearchResult[]
  companies: SearchResult[]
  questions: SearchResult[]
  notes: SearchResult[]
}

export function search(query: string) {
  return apiFetch<SearchResponse>(`/search?q=${encodeURIComponent(query)}`)
}
