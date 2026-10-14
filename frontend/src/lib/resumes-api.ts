import { apiFetch, apiFetchBlob } from '@/lib/api-client'
import type { PagedResponse } from '@/lib/applications-api'

export interface ResumeSummary {
  id: string
  name: string
  version_label: string | null
  target_role: string | null
  is_default: boolean
  has_file: boolean
  parent_resume_id: string | null
  created_at: string
  updated_at: string
}

export interface ResumeDetail {
  id: string
  name: string
  version_label: string | null
  target_role: string | null
  summary: string | null
  is_default: boolean
  parent_resume_id: string | null
  has_file: boolean
  file_original_name: string | null
  file_size_bytes: number | null
  file_mime: string | null
  file_uploaded_at: string | null
  created_at: string
  updated_at: string
}

export interface ResumeFormInput {
  name: string
  version_label?: string | null
  target_role?: string | null
  summary?: string | null
  is_default?: boolean
}

export interface ListResumesParams {
  page?: number
  size?: number
  sort?: 'created_at' | 'name'
  direction?: 'asc' | 'desc'
}

export function listResumes(params: ListResumesParams = {}) {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  if (params.sort) query.set('sort', params.sort)
  if (params.direction) query.set('direction', params.direction)
  const queryString = query.toString()
  return apiFetch<PagedResponse<ResumeSummary>>(`/resumes${queryString ? `?${queryString}` : ''}`)
}

export function getResume(id: string) {
  return apiFetch<ResumeDetail>(`/resumes/${id}`)
}

export function createResume(input: ResumeFormInput) {
  return apiFetch<ResumeDetail>('/resumes', { method: 'POST', body: input })
}

export function updateResume(id: string, input: Partial<ResumeFormInput>) {
  return apiFetch<ResumeDetail>(`/resumes/${id}`, { method: 'PATCH', body: input })
}

export function deleteResume(id: string) {
  return apiFetch<void>(`/resumes/${id}`, { method: 'DELETE' })
}

export function uploadResumeFile(id: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return apiFetch<ResumeDetail>(`/resumes/${id}/file`, { method: 'POST', body: formData })
}

export async function downloadResumeFile(id: string): Promise<{ blob: Blob; filename: string | null }> {
  return apiFetchBlob(`/resumes/${id}/file`)
}

export function cloneResume(id: string) {
  return apiFetch<ResumeDetail>(`/resumes/${id}/clone`, { method: 'POST' })
}
