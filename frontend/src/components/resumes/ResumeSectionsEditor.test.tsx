import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ResumeSectionsEditor } from '@/components/resumes/ResumeSectionsEditor'
import {
  addResumeSection,
  deleteResumeSection,
  listResumeSections,
  reorderResumeSections,
  updateResumeSection,
  type ResumeSection,
} from '@/lib/resumes-api'

vi.mock('@/lib/resumes-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/resumes-api')>('@/lib/resumes-api')
  return {
    ...actual,
    listResumeSections: vi.fn(),
    addResumeSection: vi.fn(),
    updateResumeSection: vi.fn(),
    deleteResumeSection: vi.fn(),
    reorderResumeSections: vi.fn(),
  }
})

function section(overrides: Partial<ResumeSection>): ResumeSection {
  return {
    id: 's-1',
    section_type: 'experience',
    title: 'Backend Engineer',
    organisation: 'Acme',
    start_date: null,
    end_date: null,
    description: null,
    sort_order: 0,
    ...overrides,
  }
}

function renderEditor() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ResumeSectionsEditor resumeId="resume-1" summary={null} />
    </QueryClientProvider>,
  )
}

describe('ResumeSectionsEditor', () => {
  beforeEach(() => {
    vi.mocked(listResumeSections).mockReset()
    vi.mocked(addResumeSection).mockReset()
    vi.mocked(updateResumeSection).mockReset()
    vi.mocked(deleteResumeSection).mockReset()
    vi.mocked(reorderResumeSections).mockReset()
    vi.mocked(listResumeSections).mockResolvedValue([
      section({}),
      section({ id: 's-2', section_type: 'skill', title: 'Languages', organisation: null, sort_order: 1 }),
    ])
  })

  it('lists the entries with their type', async () => {
    renderEditor()
    expect(await screen.findByText('Backend Engineer at Acme')).toBeInTheDocument()
    expect(screen.getByText('Languages')).toBeInTheDocument()
  })

  it('adds an entry with blank fields sent as null', async () => {
    vi.mocked(addResumeSection).mockResolvedValue(section({ id: 's-3' }))
    renderEditor()
    await screen.findByText('Languages')

    await userEvent.click(screen.getByRole('button', { name: 'Add entry' }))
    await userEvent.type(screen.getByLabelText('Title'), 'Platform Engineer')
    const buttons = screen.getAllByRole('button', { name: 'Add entry' })
    await userEvent.click(buttons[buttons.length - 1]!)

    await waitFor(() =>
      expect(addResumeSection).toHaveBeenCalledWith('resume-1', {
        section_type: 'experience',
        title: 'Platform Engineer',
        organisation: null,
        start_date: null,
        end_date: null,
        description: null,
      }),
    )
  })

  it('edits an entry', async () => {
    vi.mocked(updateResumeSection).mockResolvedValue(section({ title: 'Staff Engineer' }))
    renderEditor()
    await screen.findByText('Languages')

    await userEvent.click(screen.getByRole('button', { name: 'Edit Backend Engineer' }))
    const title = screen.getByLabelText('Title')
    await userEvent.clear(title)
    await userEvent.type(title, 'Staff Engineer')
    await userEvent.click(screen.getByRole('button', { name: 'Save entry' }))

    await waitFor(() =>
      expect(updateResumeSection).toHaveBeenCalledWith(
        'resume-1',
        's-1',
        expect.objectContaining({ title: 'Staff Engineer', organisation: 'Acme' }),
      ),
    )
  })

  it('moves an entry down by sending the new order', async () => {
    vi.mocked(reorderResumeSections).mockResolvedValue([])
    renderEditor()
    await screen.findByText('Languages')

    await userEvent.click(screen.getByRole('button', { name: 'Move Backend Engineer down' }))

    await waitFor(() => expect(reorderResumeSections).toHaveBeenCalledWith('resume-1', ['s-2', 's-1']))
  })

  it('disables moving the first entry up and the last entry down', async () => {
    renderEditor()
    await screen.findByText('Languages')

    expect(screen.getByRole('button', { name: 'Move Backend Engineer up' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Move Languages down' })).toBeDisabled()
  })

  it('deletes an entry', async () => {
    vi.mocked(deleteResumeSection).mockResolvedValue(undefined)
    renderEditor()
    await screen.findByText('Languages')

    await userEvent.click(screen.getByRole('button', { name: 'Delete Languages' }))

    await waitFor(() => expect(deleteResumeSection).toHaveBeenCalledWith('resume-1', 's-2'))
  })
})
