import { apiFetch, ApiError } from '@/lib/api-client'
import type { CompanySummary, PagedResponse } from '@/lib/applications-api'
import type { QuestionBankItem } from '@/lib/prep-api'

export type InterviewRoundType = 'OA' | 'PHONE_SCREEN' | 'TECHNICAL' | 'SYSTEM_DESIGN' | 'BEHAVIOURAL' | 'HR' | 'FINAL'

export type InterviewMode = 'onsite' | 'video' | 'phone'
export type InterviewOutcome = 'passed' | 'failed' | 'pending' | 'unknown'

export type QuestionCategory = 'dsa' | 'technical' | 'behavioural' | 'hr' | 'system_design' | 'company_specific'
export type QuestionDifficulty = 'easy' | 'medium' | 'hard'

export interface InterviewSummary {
  id: string
  application_id: string
  company_name: string
  job_title: string
  round_number: number
  round_type: InterviewRoundType
  scheduled_at: string | null
  completed_at: string | null
  mode: InterviewMode | null
  outcome: InterviewOutcome
  has_feedback: boolean
  created_at: string
}

export interface InterviewDetail {
  id: string
  application_id: string
  company: CompanySummary
  job_title: string
  scheduled_at: string | null
  completed_at: string | null
  round_number: number
  round_type: InterviewRoundType
  duration_minutes: number | null
  mode: InterviewMode | null
  interviewer_name: string | null
  interviewer_role: string | null
  outcome: InterviewOutcome
  perceived_difficulty: number | null
  notes: string | null
  has_feedback: boolean
  created_at: string
  updated_at: string
}

export interface InterviewFormInput {
  application_id?: string
  scheduled_at?: string | null
  completed_at?: string | null
  round_number: number
  round_type: InterviewRoundType
  duration_minutes?: number | null
  mode?: InterviewMode | null
  interviewer_name?: string | null
  interviewer_role?: string | null
  outcome?: InterviewOutcome
  perceived_difficulty?: number | null
  notes?: string | null
}

export interface InterviewQuestion {
  id: string
  interview_id: string
  question_text: string
  topic_id: string | null
  category: QuestionCategory
  difficulty: QuestionDifficulty | null
  self_rating: number | null
  time_taken_minutes: number | null
  linked_question_id: string | null
  notes: string | null
  created_at: string
  updated_at: string
}

export interface InterviewQuestionInput {
  question_text: string
  category: QuestionCategory
  difficulty?: QuestionDifficulty | null
  self_rating?: number | null
  time_taken_minutes?: number | null
  notes?: string | null
}

export interface InterviewFeedback {
  id: string
  interview_id: string
  self_rating: number | null
  what_went_well: string | null
  what_went_poorly: string | null
  improvement_areas: string | null
  external_feedback: string | null
  is_draft: boolean
  submitted_at: string | null
  created_at: string
  updated_at: string
}

export interface InterviewFeedbackInput {
  self_rating?: number | null
  what_went_well?: string | null
  what_went_poorly?: string | null
  improvement_areas?: string | null
  external_feedback?: string | null
  is_draft: boolean
}

export interface ListInterviewsParams {
  application_id?: string
  upcoming?: boolean
  round_type?: InterviewRoundType
  page?: number
  size?: number
  sort?: 'scheduled_at' | 'round_number' | 'created_at'
  direction?: 'asc' | 'desc'
}

export function listInterviews(params: ListInterviewsParams = {}) {
  const query = new URLSearchParams()
  if (params.application_id) query.set('application_id', params.application_id)
  if (params.upcoming !== undefined) query.set('upcoming', String(params.upcoming))
  if (params.round_type) query.set('round_type', params.round_type)
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.sort) query.set('sort', params.sort)
  if (params.direction) query.set('direction', params.direction)
  const queryString = query.toString()
  return apiFetch<PagedResponse<InterviewSummary>>(`/interviews${queryString ? `?${queryString}` : ''}`)
}

export function getInterview(id: string) {
  return apiFetch<InterviewDetail>(`/interviews/${id}`)
}

export function createInterview(input: InterviewFormInput) {
  return apiFetch<InterviewDetail>('/interviews', { method: 'POST', body: input })
}

export function updateInterview(id: string, input: Partial<InterviewFormInput>) {
  return apiFetch<InterviewDetail>(`/interviews/${id}`, { method: 'PATCH', body: input })
}

export function deleteInterview(id: string) {
  return apiFetch<void>(`/interviews/${id}`, { method: 'DELETE' })
}

export function listInterviewQuestions(interviewId: string) {
  return apiFetch<InterviewQuestion[]>(`/interviews/${interviewId}/questions`)
}

export function addInterviewQuestion(interviewId: string, input: InterviewQuestionInput) {
  return apiFetch<InterviewQuestion>(`/interviews/${interviewId}/questions`, { method: 'POST', body: input })
}

export function deleteInterviewQuestion(questionId: string) {
  return apiFetch<void>(`/interview-questions/${questionId}`, { method: 'DELETE' })
}

export function saveInterviewQuestionToBank(questionId: string, topicId: string | null) {
  return apiFetch<QuestionBankItem>(`/interview-questions/${questionId}/save-to-bank`, {
    method: 'POST',
    body: topicId ? { topic_id: topicId } : {},
  })
}

export async function getInterviewFeedbackOrNull(interviewId: string): Promise<InterviewFeedback | null> {
  try {
    return await apiFetch<InterviewFeedback>(`/interviews/${interviewId}/feedback`)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return null
    }
    throw error
  }
}

export function upsertInterviewFeedback(interviewId: string, input: InterviewFeedbackInput) {
  return apiFetch<InterviewFeedback>(`/interviews/${interviewId}/feedback`, { method: 'PUT', body: input })
}
