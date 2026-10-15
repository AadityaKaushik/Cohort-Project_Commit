import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SampleDataBanner } from '@/components/onboarding/SampleDataBanner'
import { LoadSampleDataButton } from '@/components/onboarding/LoadSampleDataButton'
import * as api from '@/lib/sample-data-api'

vi.mock('@/lib/sample-data-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/sample-data-api')>('@/lib/sample-data-api')
  return { ...actual, getSampleData: vi.fn(), loadSampleData: vi.fn(), removeSampleData: vi.fn() }
})

function wrap(ui: React.ReactNode) {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{ui}</QueryClientProvider>)
}

describe('sample data controls', () => {
  beforeEach(() => {
    vi.mocked(api.getSampleData).mockReset()
    vi.mocked(api.loadSampleData).mockReset()
    vi.mocked(api.removeSampleData).mockReset()
  })

  it('shows nothing when no sample data is loaded', async () => {
    vi.mocked(api.getSampleData).mockResolvedValue({ loaded: false, applications: 0, interviews: 0, contacts: 0 })
    wrap(<SampleDataBanner />)
    await waitFor(() => expect(api.getSampleData).toHaveBeenCalled())
    expect(screen.queryByRole('button', { name: 'Remove sample data' })).not.toBeInTheDocument()
  })

  it('removes sample data from the banner', async () => {
    const user = userEvent.setup()
    vi.mocked(api.getSampleData).mockResolvedValue({ loaded: true, applications: 5, interviews: 4, contacts: 2 })
    vi.mocked(api.removeSampleData).mockResolvedValue({ loaded: false, applications: 0, interviews: 0, contacts: 0 })
    wrap(<SampleDataBanner />)

    await user.click(await screen.findByRole('button', { name: 'Remove sample data' }))

    await waitFor(() => expect(api.removeSampleData).toHaveBeenCalled())
  })

  it('loads sample data from the button', async () => {
    const user = userEvent.setup()
    const onLoaded = vi.fn()
    vi.mocked(api.loadSampleData).mockResolvedValue({ loaded: true, applications: 5, interviews: 4, contacts: 2 })
    wrap(<LoadSampleDataButton onLoaded={onLoaded} />)

    await user.click(screen.getByRole('button', { name: 'Load sample data' }))

    await waitFor(() => expect(onLoaded).toHaveBeenCalled())
  })
})
