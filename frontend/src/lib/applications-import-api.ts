import { apiFetch } from '@/lib/api-client'
import type { ApplicationStatusValue } from '@/lib/applications-api'

export const IMPORT_MAX_BYTES = 1024 * 1024

export interface ImportColumn {
  header: string
  field: 'company' | 'role' | 'status' | 'appliedDate' | 'location' | 'url' | 'notes' | 'salary' | null
}

export interface ImportRow {
  line: number
  company: string
  role: string
  status: ApplicationStatusValue | null
  date_applied: string | null
  location: string | null
  url: string | null
  outcome: 'import' | 'duplicate' | 'invalid'
  warnings: string[]
}

export interface ImportResult {
  dry_run: boolean
  source: string
  total: number
  importable: number
  duplicates: number
  invalid: number
  created: number
  columns: ImportColumn[]
  rows: ImportRow[]
}

export function importApplicationsCsv(file: File, dryRun: boolean) {
  const formData = new FormData()
  formData.append('file', file)
  return apiFetch<ImportResult>(`/applications/import?dryRun=${dryRun}`, { method: 'POST', body: formData })
}
