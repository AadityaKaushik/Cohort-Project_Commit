import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReportsPage } from '@/pages/reports/ReportsPage'
import { createReport, deleteReport, downloadReport, listReports, type ReportExport } from '@/lib/reports-api'
import { saveBlob } from '@/lib/download'

vi.mock('@/lib/reports-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/reports-api')>('@/lib/reports-api')
  return { ...actual, listReports: vi.fn(), createReport: vi.fn(), downloadReport: vi.fn(), deleteReport: vi.fn() }
})
vi.mock('@/lib/download', () => ({ saveBlob: vi.fn() }))

function report(overrides: Partial<ReportExport> = {}): ReportExport {
  return {
    id: 'r-1',
    report_type: 'applications',
    format: 'csv',
    date_from: null,
    date_to: null,
    file_name: 'cohort-applications-2026-10-12.csv',
    file_size_bytes: 2048,
    generated_at: '2026-10-12T10:00:00Z',
    expires_at: '2026-10-19T10:00:00Z',
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ReportsPage />
    </QueryClientProvider>,
  )
}

describe('ReportsPage', () => {
  afterEach(() => vi.clearAllMocks())

  it('shows an empty state when there are no exports', async () => {
    vi.mocked(listReports).mockResolvedValue([])
    renderPage()
    expect(await screen.findByText('No exports yet')).toBeInTheDocument()
  })

  it('generates an export with the chosen type, format and range', async () => {
    vi.mocked(listReports).mockResolvedValue([])
    vi.mocked(createReport).mockResolvedValue(report())
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Report'), 'analytics_summary')
    await user.type(screen.getByLabelText('Applied from'), '2026-07-01')
    await user.click(screen.getByRole('button', { name: 'Generate export' }))

    await waitFor(() =>
      expect(createReport).toHaveBeenCalledWith({ report_type: 'analytics_summary', format: 'csv', date_from: '2026-07-01' }),
    )
  })

  it('offers only PDF for a progress report', async () => {
    vi.mocked(listReports).mockResolvedValue([])
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Report'), 'progress_report')

    const formatSelect = screen.getByLabelText('Format') as HTMLSelectElement
    expect(formatSelect.value).toBe('pdf')
    expect(Array.from(formatSelect.options).map((option) => option.value)).toEqual(['pdf'])
  })

  it('downloads and deletes a listed export', async () => {
    vi.mocked(listReports).mockResolvedValue([report()])
    vi.mocked(downloadReport).mockResolvedValue({ blob: new Blob(['a']), filename: 'x.csv' })
    vi.mocked(deleteReport).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('cohort-applications-2026-10-12.csv')
    await user.click(screen.getByRole('button', { name: /download/i }))
    await waitFor(() => expect(saveBlob).toHaveBeenCalledWith(expect.any(Blob), 'x.csv'))

    await user.click(screen.getByRole('button', { name: /delete cohort-applications/i }))
    await waitFor(() => expect(deleteReport).toHaveBeenCalledWith('r-1'))
  })
})
