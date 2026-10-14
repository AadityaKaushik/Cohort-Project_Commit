import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AiDisclosure } from '@/components/ai/AiDisclosure'
import { getAiStatus } from '@/lib/ai-api'

vi.mock('@/lib/ai-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/ai-api')>('@/lib/ai-api')
  return { ...actual, getAiStatus: vi.fn() }
})

function renderNotice() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AiDisclosure />
    </QueryClientProvider>,
  )
}

describe('AiDisclosure', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('names Gemini when the hosted provider is active', async () => {
    vi.mocked(getAiStatus).mockResolvedValue({ provider: 'gemini', hosted: true })
    renderNotice()

    expect(await screen.findByText('Gemini is active')).toBeInTheDocument()
  })

  it('shows nothing for the local model', async () => {
    vi.mocked(getAiStatus).mockResolvedValue({ provider: 'ollama', hosted: false })
    renderNotice()

    await vi.waitFor(() => expect(getAiStatus).toHaveBeenCalled())
    expect(screen.queryByText('Gemini is active')).not.toBeInTheDocument()
  })
})
