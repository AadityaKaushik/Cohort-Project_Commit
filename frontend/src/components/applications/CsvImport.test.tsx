import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CsvImport } from '@/components/applications/CsvImport'
import { ApiError } from '@/lib/api-client'
import * as importApi from '@/lib/applications-import-api'

vi.mock('@/lib/applications-import-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-import-api')>('@/lib/applications-import-api')
  return { ...actual, importApplicationsCsv: vi.fn() }
})

const preview: importApi.ImportResult = {
  dry_run: true,
  total: 3,
  importable: 1,
  duplicates: 1,
  invalid: 1,
  created: 0,
  columns: [
    { header: 'Company', field: 'company' },
    { header: 'Job Title', field: 'role' },
    { header: 'Salary', field: null },
  ],
  rows: [
    { line: 2, company: 'Globex', role: 'Analyst', status: 'SAVED', date_applied: '2026-01-06', location: null, url: null, outcome: 'import', warnings: ['Unknown status "ghosted", saved as Saved.'] },
    { line: 3, company: '', role: 'Intern', status: null, date_applied: null, location: null, url: null, outcome: 'invalid', warnings: ['Company and role are both required.'] },
    { line: 4, company: 'Acme', role: 'Engineer', status: 'APPLIED', date_applied: '2026-01-05', location: null, url: null, outcome: 'duplicate', warnings: [] },
  ],
}

function renderImport(onImported = vi.fn()) {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <CsvImport onImported={onImported} />
    </QueryClientProvider>,
  )
  return onImported
}

function csvFile() {
  return new File(['company,role\nAcme,Dev\n'], 'apps.csv', { type: 'text/csv' })
}

describe('CsvImport', () => {
  beforeEach(() => {
    vi.mocked(importApi.importApplicationsCsv).mockReset()
  })

  it('shows the column mapping, counts and per-row warnings before importing', async () => {
    vi.mocked(importApi.importApplicationsCsv).mockResolvedValue(preview)
    const user = userEvent.setup()
    renderImport()

    await user.upload(screen.getByLabelText('CSV file'), csvFile())

    expect(await screen.findByText('Job Title → Role')).toBeInTheDocument()
    expect(screen.getByText('Salary → ignored')).toBeInTheDocument()
    expect(screen.getByText('1 to import, 1 duplicates, 1 skipped.')).toBeInTheDocument()
    expect(screen.getByText('Unknown status "ghosted", saved as Saved.')).toBeInTheDocument()
    expect(screen.getByText('Company and role are both required.')).toBeInTheDocument()
    expect(importApi.importApplicationsCsv).toHaveBeenCalledWith(expect.any(File), true)
  })

  it('imports for real only after the confirm button', async () => {
    vi.mocked(importApi.importApplicationsCsv)
      .mockResolvedValueOnce(preview)
      .mockResolvedValueOnce({ ...preview, dry_run: false, created: 1 })
    const user = userEvent.setup()
    const onImported = renderImport()

    await user.upload(screen.getByLabelText('CSV file'), csvFile())
    await user.click(await screen.findByRole('button', { name: 'Import 1 application' }))

    await waitFor(() => expect(onImported).toHaveBeenCalledWith(1))
    expect(importApi.importApplicationsCsv).toHaveBeenLastCalledWith(expect.any(File), false)
  })

  it('shows the server message when the file is rejected', async () => {
    vi.mocked(importApi.importApplicationsCsv).mockRejectedValue(
      new ApiError(400, 'The file needs a company column and a role or title column.', 'INVALID_IMPORT'),
    )
    const user = userEvent.setup()
    renderImport()

    await user.upload(screen.getByLabelText('CSV file'), csvFile())

    expect(await screen.findByText('The file needs a company column and a role or title column.')).toBeInTheDocument()
  })

  it('does not offer to import when nothing is importable', async () => {
    vi.mocked(importApi.importApplicationsCsv).mockResolvedValue({ ...preview, importable: 0 })
    const user = userEvent.setup()
    renderImport()

    await user.upload(screen.getByLabelText('CSV file'), csvFile())

    expect(await screen.findByRole('button', { name: 'Import 0 applications' })).toBeDisabled()
  })
})
