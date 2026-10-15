import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { EmailDraftPanel } from '@/components/ai/EmailDraftPanel'
import * as aiApi from '@/lib/ai-api'

vi.mock('@/lib/ai-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/ai-api')>('@/lib/ai-api')
  return {
    ...actual,
    getAiStatus: vi.fn().mockResolvedValue({ provider: 'ollama', hosted: false }),
    createEmailDraft: vi.fn(),
    createContactDraft: vi.fn(),
  }
})

function renderPanel(props: Partial<React.ComponentProps<typeof EmailDraftPanel>> = {}) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <EmailDraftPanel applicationId="a1" {...props} />
    </QueryClientProvider>,
  )
}

describe('EmailDraftPanel', () => {
  beforeEach(() => vi.mocked(aiApi.createEmailDraft).mockReset())

  it('requests the chosen kind and shows an editable draft', async () => {
    vi.mocked(aiApi.createEmailDraft).mockResolvedValue({
      kind: 'follow_up',
      subject: 'Following up',
      body: 'Hi there, any update?',
      source: 'model',
      ai_available: true,
    })
    renderPanel()
    await userEvent.selectOptions(screen.getByLabelText('Email type'), 'follow_up')
    await userEvent.click(screen.getByRole('button', { name: 'Create a draft' }))

    expect(aiApi.createEmailDraft).toHaveBeenCalledWith('a1', 'follow_up', undefined)
    expect(await screen.findByLabelText('Email draft')).toHaveValue('Hi there, any update?')
    expect(screen.getByLabelText('Email subject')).toHaveValue('Following up')
    expect(screen.queryByText(/AI is unavailable/)).not.toBeInTheDocument()
  })

  it('shows the unavailable state next to the template', async () => {
    vi.mocked(aiApi.createEmailDraft).mockResolvedValue({
      kind: 'negotiation',
      subject: 'Your offer',
      body: 'Hi, thank you for the offer.',
      source: 'template',
      ai_available: false,
    })
    renderPanel({ kinds: ['negotiation'] })
    await userEvent.click(screen.getByRole('button', { name: 'Create a draft' }))

    expect(await screen.findByText(/AI is unavailable, try again later/)).toBeInTheDocument()
    expect(screen.getByLabelText('Email draft')).toHaveValue('Hi, thank you for the offer.')
  })

  it('sends the chosen interview for a thank-you', async () => {
    vi.mocked(aiApi.createEmailDraft).mockResolvedValue({
      kind: 'thank_you',
      subject: 'Thanks',
      body: 'Hi Sam, thanks.',
      source: 'template',
      ai_available: true,
    })
    renderPanel({ interviews: [{ id: 'i1', round_number: 1, round_type: 'TECHNICAL' }] })
    await userEvent.selectOptions(screen.getByLabelText('Interview'), 'i1')
    await userEvent.click(screen.getByRole('button', { name: 'Create a draft' }))

    expect(aiApi.createEmailDraft).toHaveBeenCalledWith('a1', 'thank_you', 'i1')
  })

  it('shows the Gemini disclosure when the hosted provider is active', async () => {
    vi.mocked(aiApi.getAiStatus).mockResolvedValueOnce({ provider: 'gemini', hosted: true })
    renderPanel()
    expect(await screen.findByText('Gemini is active')).toBeInTheDocument()
  })
})

describe('EmailDraftPanel for a contact', () => {
  it('asks the contact endpoint when only a contact is given', async () => {
    vi.mocked(aiApi.createContactDraft).mockResolvedValue({
      kind: 'referral_request',
      subject: 'Referral for the Engineer role at Acme',
      body: 'Hi Meera, would you be open to referring me?',
      source: 'template',
      ai_available: false,
    })
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <EmailDraftPanel contactId="c1" kinds={['referral_request', 'reconnect', 'referral_thanks']} />
      </QueryClientProvider>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Create a draft' }))

    expect(aiApi.createContactDraft).toHaveBeenCalledWith('c1', 'referral_request')
    expect(await screen.findByLabelText('Email draft')).toHaveValue('Hi Meera, would you be open to referring me?')
  })
})
