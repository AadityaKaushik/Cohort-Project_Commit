import { useState, type ChangeEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { IMPORT_MAX_BYTES, importApplicationsCsv, type ImportResult } from '@/lib/applications-import-api'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { FormError } from '@/components/shared/Form'

const FIELD_LABELS: Record<string, string> = {
  company: 'Company',
  role: 'Role',
  status: 'Status',
  appliedDate: 'Applied date',
  location: 'Location',
  url: 'Job URL',
  notes: 'Notes',
  salary: 'Added to notes',
}

const OUTCOME_LABELS: Record<string, string> = {
  import: 'Will import',
  duplicate: 'Duplicate, skipped',
  invalid: 'Skipped',
}

export function CsvImport({ onImported }: { onImported?: (created: number) => void }) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<ImportResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  function failure(cause: unknown) {
    setError(cause instanceof ApiError ? cause.message : 'Could not read that file. Please try again.')
  }

  const previewMutation = useMutation({
    mutationFn: (selected: File) => importApplicationsCsv(selected, true),
    onSuccess: setPreview,
    onError: failure,
  })

  const importMutation = useMutation({
    mutationFn: () => importApplicationsCsv(file!, false),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['companies'] })
      toast({ title: `Imported ${result.created} ${result.created === 1 ? 'application' : 'applications'}` })
      setPreview(null)
      setFile(null)
      onImported?.(result.created)
    },
    onError: failure,
  })

  function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0] ?? null
    setError(null)
    setPreview(null)
    setFile(selected)
    if (!selected) return
    if (selected.size > IMPORT_MAX_BYTES) {
      setError('That file is over 1 MB. Split it into smaller files.')
      return
    }
    previewMutation.mutate(selected)
  }

  const visibleRows = preview?.rows.filter((row) => row.outcome !== 'import' || row.warnings.length > 0) ?? []

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <label htmlFor="csvFile" className="text-sm font-medium">
          CSV file
        </label>
        <input
          id="csvFile"
          type="file"
          accept=".csv,text/csv"
          onChange={handleFile}
          className="block w-full text-sm file:mr-3 file:rounded-md file:border file:bg-card file:px-3 file:py-1.5 file:text-sm"
        />
        <p className="text-xs text-muted-foreground">
          Up to 500 rows. Needs a company column and a role or title column. Status, applied date, location, URL and notes are optional. LinkedIn's Job Applications.csv and Google Sheets or Notion trackers are recognised.
        </p>
      </div>

      {previewMutation.isPending && <p role="status" className="text-sm text-muted-foreground">Reading the file…</p>}
      {error && <FormError>{error}</FormError>}

      {preview && (
        <div className="space-y-4">
          {preview.source !== 'Cohort format' && (
            <p className="text-sm font-medium">Looks like a {preview.source.toLowerCase().replace('linkedin', 'LinkedIn')}.</p>
          )}
          <section aria-label="Column mapping">
            <h4 className="text-sm font-medium">Columns</h4>
            <ul className="mt-1 flex flex-wrap gap-2 text-xs">
              {preview.columns.map((column, index) => (
                <li key={`${column.header}-${index}`} className="rounded-full border px-2.5 py-1">
                  {column.header || '(blank)'} → {column.field ? FIELD_LABELS[column.field] : 'ignored'}
                </li>
              ))}
            </ul>
          </section>

          <p className="text-sm">
            {preview.importable} to import, {preview.duplicates} duplicates, {preview.invalid} skipped.
          </p>

          {visibleRows.length > 0 && (
            <div className="overflow-x-auto rounded-lg border">
              <table className="w-full text-left text-sm">
                <caption className="sr-only">Rows that need attention</caption>
                <thead className="bg-surface-2 text-xs text-muted-foreground">
                  <tr>
                    <th className="px-3 py-2">Row</th>
                    <th className="px-3 py-2">Company</th>
                    <th className="px-3 py-2">Role</th>
                    <th className="px-3 py-2">Result</th>
                    <th className="px-3 py-2">Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleRows.map((row) => (
                    <tr key={row.line} className="border-t align-top">
                      <td className="px-3 py-2 tabular-nums">{row.line}</td>
                      <td className="px-3 py-2">{row.company || '—'}</td>
                      <td className="px-3 py-2">{row.role || '—'}</td>
                      <td className="px-3 py-2">
                        {OUTCOME_LABELS[row.outcome]}
                        {row.status && row.outcome === 'import' ? ` as ${formatLabel(row.status)}` : ''}
                      </td>
                      <td className="px-3 py-2 text-muted-foreground">{row.warnings.join(' ')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <Button disabled={preview.importable === 0 || importMutation.isPending} onClick={() => importMutation.mutate()}>
            {importMutation.isPending
              ? 'Importing…'
              : `Import ${preview.importable} ${preview.importable === 1 ? 'application' : 'applications'}`}
          </Button>
        </div>
      )}
    </div>
  )
}
