import { apiFetch } from '@/lib/api-client'

export const COMPETENCIES = [
  'leadership',
  'teamwork',
  'conflict',
  'failure',
  'ownership',
  'deadline',
  'learning',
  'impact',
  'communication',
  'problem_solving',
] as const

export type Competency = (typeof COMPETENCIES)[number]

/** Mirrors com.cohort.api.dto.StoryResponse. */
export interface Story {
  id: string
  title: string
  situation: string | null
  task: string | null
  action: string | null
  result: string | null
  competencies: Competency[]
  interview_id: string | null
  created_at: string
  updated_at: string
}

export interface StoryInput {
  title: string
  situation: string
  task: string
  action: string
  result: string
  competencies: Competency[]
  interview_id?: string
}

export const storyKeys = {
  all: ['stories'] as const,
  list: (competency: Competency | 'all') => ['stories', 'list', competency] as const,
  detail: (id: string) => ['stories', 'detail', id] as const,
}

export function listStories(competency?: Competency) {
  const query = competency ? `?competency=${competency}` : ''
  return apiFetch<Story[]>(`/stories${query}`)
}

export function getStory(id: string) {
  return apiFetch<Story>(`/stories/${id}`)
}

export function createStory(input: StoryInput) {
  return apiFetch<Story>('/stories', { method: 'POST', body: input })
}

export function updateStory(id: string, input: StoryInput) {
  return apiFetch<Story>(`/stories/${id}`, { method: 'PATCH', body: input })
}

export function deleteStory(id: string) {
  return apiFetch<void>(`/stories/${id}`, { method: 'DELETE' })
}

export function firstLine(text: string | null): string {
  return text?.split('\n').find((line) => line.trim() !== '')?.trim() ?? ''
}
