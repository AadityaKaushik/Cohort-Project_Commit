import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DownloadIcon, FileTextIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
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

const NATIVE_SELECT =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

/** US-80 / FR-102..104: generate a PDF or CSV export and download it before it expires. */
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
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Reports</h2>
        <p className="text-sm text-muted-foreground">Export your data. Files are private to you and expire automatically.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">New export</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="report-type">Report</Label>
              <select id="report-type" className={NATIVE_SELECT} value={reportType} onChange={(e) => selectType(e.target.value as ReportType)}>
                {REPORT_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
              <p className="text-xs text-muted-foreground">{selectedType.description}</p>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="report-format">Format</Label>
              <select id="report-format" className={NATIVE_SELECT} value={format} onChange={(e) => setFormat(e.target.value as ReportFormat)}>
                {selectedType.formats.map((option) => (
                  <option key={option} value={option}>
                    {option.toUpperCase()}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="report-from">Applied from</Label>
              <Input id="report-from" type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="report-to">Applied to</Label>
              <Input id="report-to" type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
            </div>
          </div>
          {error ? (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          ) : null}
          <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Generating...' : 'Generate export'}
          </Button>
        </CardContent>
      </Card>

      <section className="space-y-3" aria-label="Your exports">
        <h3 className="text-sm font-medium">Your exports</h3>
        {reportsQuery.isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : reports.length === 0 ? (
          <EmptyState icon={FileTextIcon} title="No exports yet" description="Generate one above and it will be listed here until it expires." />
        ) : (
          <ul className="space-y-2">
            {reports.map((report) => (
              <li key={report.id} className="flex items-center justify-between gap-3 rounded-lg border p-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{report.file_name}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatFileSize(report.file_size_bytes)} - expires {new Date(report.expires_at).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <Button variant="outline" size="sm" onClick={() => downloadMutation.mutate(report.id)} disabled={downloadMutation.isPending}>
                    <DownloadIcon className="h-4 w-4" />
                    Download
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Delete ${report.file_name}`}
                    onClick={() => deleteMutation.mutate(report.id)}
                    disabled={deleteMutation.isPending}
                  >
                    <Trash2Icon className="h-4 w-4" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
