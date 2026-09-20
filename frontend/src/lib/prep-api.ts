import { apiFetch } from '@/lib/api-client'

/** Mirrors com.cohort.api.entity.Topic.Category. */
export type TopicCategory = 'dsa' | 'system_design' | 'databases' | 'os_networking' | 'oop' | 'behavioural'

/** Mirrors com.cohort.api.entity.Difficulty. */
export type Difficulty = 'easy' | 'medium' | 'hard'

/** Mirrors com.cohort.api.entity.Question.Category. */
export type QuestionCategory = 'dsa' | 'technical' | 'behavioural' | 'hr' | 'system_design' | 'company_specific'

/** Mirrors com.cohort.api.entity.PracticeAttempt.Status. */
export type PracticeAttemptStatus = 'not_started' | 'in_progress' | 'confident' | 'needs_review'

/** Mirrors com.cohort.api.entity.PreparationTask.Status. */
export type PreparationTaskStatus = 'open' | 'completed'

/** Mirrors com.cohort.api.dto.TopicResponse (US-38). */
export interface Topic {
  id: string
  name: string
  slug: string
  category: TopicCategory
  description: string | null
  default_difficulty: Difficulty
  taxonomy_version: number
  created_at: string
}

/** Mirrors com.cohort.api.dto.TopicGraphResponse. */
export interface TopicGraph {
  nodes: Topic[]
  edges: { topic_id: string; prerequisite_topic_id: string }[]
}

/** Mirrors com.cohort.api.dto.QuestionResponse. */
export interface QuestionBankItem {
  id: string
  topic_id: string
  topic_name: string | null
  question_text: string
  category: QuestionCategory
  difficulty: Difficulty | null
  is_system: boolean
  source_question_id: string | null
  created_at: string
  updated_at: string
}

export interface PagedResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

/** Mirrors com.cohort.api.dto.PracticeAttemptResponse. */
export interface PracticeAttempt {
  id: string
  question_id: string
  status: PracticeAttemptStatus
  confidence: number | null
  time_taken_minutes: number | null
  notes: string | null
  attempted_at: string
  created_at: string
}

/** Mirrors com.cohort.api.dto.PreparationTaskResponse (US-40). */
export interface PreparationTask {
  id: string
  topic_id: string
  topic_name: string | null
  title: string
  description: string | null
  due_date: string | null
  estimated_effort_minutes: number | null
  status: PreparationTaskStatus
  completed_at: string | null
  created_at: string
  updated_at: string
}

export interface PreparationTaskInput {
  topic_id: string
  title: string
  description?: string | null
  due_date?: string | null
  estimated_effort_minutes?: number | null
}

/** Mirrors com.cohort.api.dto.TopicMasteryView (US-41). */
export interface TopicMasteryView {
  topic_id: string
  topic_name: string
  slug: string
  category: TopicCategory
  mastery_score: number
  observation_count: number
  sufficient_evidence: boolean
}

/** Mirrors com.cohort.api.dto.CategoryProgress. */
export interface CategoryProgress {
  category: TopicCategory
  total_topics: number
  tasks_total: number
  tasks_completed: number
  average_mastery: number | null
}

/** Mirrors com.cohort.api.dto.PreparationProgressResponse. */
export interface PreparationProgress {
  categories: CategoryProgress[]
  topics: TopicMasteryView[]
}

export function listTopics() {
  return apiFetch<Topic[]>('/topics')
}

export function getTopicGraph() {
  return apiFetch<TopicGraph>('/topics/graph')
}

export interface ListQuestionsParams {
  category?: QuestionCategory
  topic_id?: string
  difficulty?: Difficulty
  is_system?: boolean
  q?: string
  page?: number
  size?: number
}

export function listQuestions(params: ListQuestionsParams = {}) {
  const query = new URLSearchParams()
  if (params.category) query.set('category', params.category)
  if (params.topic_id) query.set('topic_id', params.topic_id)
  if (params.difficulty) query.set('difficulty', params.difficulty)
  if (params.is_system !== undefined) query.set('is_system', String(params.is_system))
  if (params.q) query.set('q', params.q)
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  const queryString = query.toString()
  return apiFetch<PagedResponse<QuestionBankItem>>(`/questions${queryString ? `?${queryString}` : ''}`)
}

export function recordPracticeAttempt(
  questionId: string,
  input: { status: PracticeAttemptStatus; confidence?: number | null; time_taken_minutes?: number | null; notes?: string | null },
) {
  return apiFetch<PracticeAttempt>(`/questions/${questionId}/attempts`, { method: 'POST', body: input })
}

export function listPracticeAttempts(questionId: string) {
  return apiFetch<PracticeAttempt[]>(`/questions/${questionId}/attempts`)
}

export function listPreparationTasks() {
  return apiFetch<PreparationTask[]>('/preparation/tasks')
}

export function createPreparationTask(input: PreparationTaskInput) {
  return apiFetch<PreparationTask>('/preparation/tasks', { method: 'POST', body: input })
}

export function updatePreparationTask(id: string, input: Partial<PreparationTaskInput & { status: PreparationTaskStatus }>) {
  return apiFetch<PreparationTask>(`/preparation/tasks/${id}`, { method: 'PATCH', body: input })
}

export function deletePreparationTask(id: string) {
  return apiFetch<void>(`/preparation/tasks/${id}`, { method: 'DELETE' })
}

export function getPreparationProgress() {
  return apiFetch<PreparationProgress>('/preparation/progress')
}
