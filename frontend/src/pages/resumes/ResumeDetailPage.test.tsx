import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ResumeDetailPage } from '@/pages/resumes/ResumeDetailPage'
import { getResume, type ResumeDetail } from '@/lib/resumes-api'
import { setTokens, clearTokens } from '@/lib/auth-storage'

// Only getResume/cloneResume/deleteResume are stubbed directly; uploadResumeFile
// is left as the real implementation so the test below exercises the actual
// FormData construction against a mocked `fetch`, proving the request shape
// matches ResumeController's `@RequestPart("file") MultipartFile` contract
// (P20) — a single part literally named "file".
vi.mock('@/lib/resumes-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/resumes-api')>('@/lib/resumes-api')
  return {
    ...actual,
    getResume: vi.fn(),
    cloneResume: vi.fn(),
    deleteResume: vi.fn(),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/resumes/resume-1']}>
        <Routes>
          <Route path="/resumes/:id" element={<ResumeDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleDetail(overrides: Partial<ResumeDetail> = {}): ResumeDetail {
  return {
    id: 'resume-1',
    name: 'Backend Resume',
    version_label: 'v1',
    target_role: 'Backend Engineer',
    summary: null,
    is_default: false,
    parent_resume_id: null,
    has_file: false,
    file_original_name: null,
    file_size_bytes: null,
    file_mime: null,
    file_uploaded_at: null,
    created_at: '2026-01-10T00:00:00Z',
    updated_at: '2026-01-10T00:00:00Z',
    ...overrides,
  }
}

describe('ResumeDetailPage', () => {
  beforeEach(() => {
    vi.mocked(getResume).mockReset()
    setTokens('access-1', 'refresh-1')
  })

  afterEach(() => {
    clearTokens()
    vi.restoreAllMocks()
  })

  it('shows an empty-state when the resume is missing or foreign (404)', async () => {
    vi.mocked(getResume).mockRejectedValue(new Error('not found'))

    renderPage()

    expect(await screen.findByText('Resume not found')).toBeInTheDocument()
  })

  it('shows file details and a download action once a file has been uploaded', async () => {
    vi.mocked(getResume).mockResolvedValue(
      sampleDetail({ has_file: true, file_original_name: 'resume.pdf', file_size_bytes: 2048 }),
    )

    renderPage()

    expect(await screen.findByText('resume.pdf')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /download/i })).toBeInTheDocument()
  })

  it('uploads the chosen file as multipart/form-data with a "file" part, matching the backend contract', async () => {
    vi.mocked(getResume).mockResolvedValue(sampleDetail())

    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify(sampleDetail({ has_file: true, file_original_name: 'resume.pdf', file_size_bytes: 10 })),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    renderPage()
    await screen.findByText('No file uploaded yet.')

    const file = new File(['%PDF-1.4 fake resume'], 'resume.pdf', { type: 'application/pdf' })
    const input = screen.getByTestId('upload-dropzone-input') as HTMLInputElement

    await userEvent.upload(input, file)

    await waitFor(() => expect(fetchMock).toHaveBeenCalled())

    const [url, init] = fetchMock.mock.calls[0]!
    expect(String(url)).toContain('/resumes/resume-1/file')
    expect(init?.method).toBe('POST')

    const body = init?.body as FormData
    expect(body).toBeInstanceOf(FormData)
    const uploadedFile = body.get('file') as File
    expect(uploadedFile.name).toBe('resume.pdf')

    // The interceptor must not force a JSON Content-Type onto a multipart
    // body — the browser needs to set its own boundary.
    const headers = init?.headers as Record<string, string> | undefined
    expect(headers?.['Content-Type']).toBeUndefined()
  })
})
