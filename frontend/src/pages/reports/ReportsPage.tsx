import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DownloadIcon, FileDownIcon, FileTextIcon, Loader2Icon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { EmptyState } from '@/components/shared/EmptyState'
import { NativeSelect } from '@/components/ui/native-select'
import { FormActions, FormError, FormField } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { ApiError } from '@/lib/api-client'
import { saveBlob } from '@/lib/download'
import {
  REPORT_TYPES,
  createReport,
  deleteReport,
  downloadReport,
  formatFileSize,
  listReports,
  reportKeys,
  type ReportFormat,
  type ReportType,
} from '@/lib/reports-api'

export function ReportsPage() {
  const queryClient = useQueryClient()
  const [reportType, setReportType] = useState<ReportType>('applications')
  const [format, setFormat] = useState<ReportFormat>('csv')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [error, setError] = useState<string | null>(null)

  const selectedType = REPORT_TYPES.find((type) => type.value === reportType)!
  const reportsQuery = useQuery({ queryKey: reportKeys.all, queryFn: listReports })

  const createMutation = useMutation({
    mutationFn: () =>
      createReport({
        report_type: reportType,
        format,
        ...(dateFrom ? { date_from: dateFrom } : {}),
        ...(dateTo ? { date_to: dateTo } : {}),
      }),
    onSuccess: () => {
      setError(null)
      queryClient.invalidateQueries({ queryKey: reportKeys.all })
    },
    onError: (cause: unknown) => setError(cause instanceof ApiError ? cause.message : 'Could not generate the report.'),
  })

  const downloadMutation = useMutation({
    mutationFn: (id: string) => downloadReport(id),
    onSuccess: ({ blob, filename }) => saveBlob(blob, filename ?? 'report'),
    onError: (cause: unknown) => {
      setError(cause instanceof ApiError ? cause.message : 'Could not download the report.')
      queryClient.invalidateQueries({ queryKey: reportKeys.all })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteReport(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reportKeys.all }),
  })

  function selectType(next: ReportType) {
    setReportType(next)
    const allowed = REPORT_TYPES.find((type) => type.value === next)!.formats
    if (!allowed.includes(format)) setFormat(allowed[0]!)
  }

  const reports = reportsQuery.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Reports" description="Export your data. Files are private to you and expire automatically." />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <SectionCard title="New export" description="Choose what to export and in which format.">
          <div className="space-y-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField id="report-type" label="Report" hint={selectedType.description}>
                <NativeSelect id="report-type" value={reportType} onChange={(e) => selectType(e.target.value as ReportType)}>
                  {REPORT_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
              <FormField id="report-format" label="Format">
                <NativeSelect id="report-format" value={format} onChange={(e) => setFormat(e.target.value as ReportFormat)}>
                  {selectedType.formats.map((option) => (
                    <option key={option} value={option}>
                      {option.toUpperCase()}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
              <FormField id="report-from" label="Applied from">
                <Input id="report-from" type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
              </FormField>
              <FormField id="report-to" label="Applied to">
                <Input id="report-to" type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
              </FormField>
            </div>
            {error && <FormError>{error}</FormError>}
            <FormActions>
              <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>
                {createMutation.isPending ? <Loader2Icon aria-hidden className="animate-spin" /> : <FileDownIcon aria-hidden />}
                {createMutation.isPending ? 'Generating...' : 'Generate export'}
              </Button>
            </FormActions>
          </div>
        </SectionCard>

        <section className="space-y-3" aria-label="Your exports">
          <h3 className="font-heading text-base font-semibold tracking-tight">Your exports</h3>
          {reportsQuery.isLoading ? (
            <LoadingRows rows={2} label="Loading exports" />
          ) : reportsQuery.isError ? (
            <ErrorState title="Couldn't load your exports" error={reportsQuery.error} onRetry={() => reportsQuery.refetch()} />
          ) : reports.length === 0 ? (
            <EmptyState compact icon={FileTextIcon} title="No exports yet" description="Generate one and it will be listed here until it expires." />
          ) : (
            <ul className="stagger space-y-2">
              {reports.map((report) => (
                <li key={report.id} className="surface-raised flex items-center justify-between gap-3 rounded-xl border p-3">
                  <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-info/10 text-info">
                    <FileTextIcon className="size-4" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{report.file_name}</p>
                    <p className="text-xs text-muted-foreground">
                      {formatFileSize(report.file_size_bytes)} · expires {new Date(report.expires_at).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <Button variant="outline" size="sm" onClick={() => downloadMutation.mutate(report.id)} disabled={downloadMutation.isPending}>
                      <DownloadIcon aria-hidden />
                      Download
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Delete ${report.file_name}`}
                      onClick={() => deleteMutation.mutate(report.id)}
                      disabled={deleteMutation.isPending}
                    >
                      <Trash2Icon aria-hidden />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
