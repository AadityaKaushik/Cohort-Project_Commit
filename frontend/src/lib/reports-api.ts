import { apiFetch, apiFetchBlob } from '@/lib/api-client'

export type ReportType = 'applications' | 'analytics_summary' | 'progress_report'
export type ReportFormat = 'pdf' | 'csv'

export interface ReportExport {
  id: string
  report_type: ReportType
  format: ReportFormat
  date_from: string | null
  date_to: string | null
  file_name: string
  file_size_bytes: number
  generated_at: string
  expires_at: string
}

export interface CreateReportInput {
  report_type: ReportType
  format: ReportFormat
  date_from?: string
  date_to?: string
}

export const REPORT_TYPES: { value: ReportType; label: string; description: string; formats: ReportFormat[] }[] = [
  { value: 'applications', label: 'Applications', description: 'Every application with its stage, dates and interviews.', formats: ['csv', 'pdf'] },
  { value: 'analytics_summary', label: 'Analytics summary', description: 'Funnel, conversion rates, rejections and stage dwell times.', formats: ['csv', 'pdf'] },
  { value: 'progress_report', label: 'Progress report', description: 'Applications and analytics together in one document.', formats: ['pdf'] },
]

export function listReports() {
  return apiFetch<ReportExport[]>('/reports')
}

export function createReport(input: CreateReportInput) {
  return apiFetch<ReportExport>('/reports', { method: 'POST', body: input })
}

export function downloadReport(id: string) {
  return apiFetchBlob(`/reports/${id}/file`)
}

export function deleteReport(id: string) {
  return apiFetch<void>(`/reports/${id}`, { method: 'DELETE' })
}

export const reportKeys = { all: ['reports'] as const }

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
