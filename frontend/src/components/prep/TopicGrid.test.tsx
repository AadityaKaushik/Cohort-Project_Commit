import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TopicGrid } from '@/components/prep/TopicGrid'
import type { TopicMasteryView } from '@/lib/prep-api'

function topic(overrides: Partial<TopicMasteryView> = {}): TopicMasteryView {
  return {
    topic_id: 'topic-1',
    topic_name: 'Arrays',
    slug: 'arrays',
    category: 'dsa',
    mastery_score: 0.8,
    observation_count: 6,
    sufficient_evidence: true,
    ...overrides,
  }
}

describe('TopicGrid', () => {
  it('shows a message when there are no topics', () => {
    render(<TopicGrid topics={[]} />)
    expect(screen.getByText('No topics yet.')).toBeInTheDocument()
  })

  it('renders a mastery badge that never relies on colour alone', () => {
    render(<TopicGrid topics={[topic()]} />)
    expect(screen.getByText('Arrays')).toBeInTheDocument()
    // The label text always carries the number and a plain-language band, not colour alone (US-41).
    expect(screen.getByTitle('80% mastery from 6 observations')).toBeInTheDocument()
  })

  it('shows insufficient data instead of a number below the observation threshold', () => {
    render(<TopicGrid topics={[topic({ sufficient_evidence: false, observation_count: 1 })]} />)
    expect(screen.getByText('Insufficient data')).toBeInTheDocument()
  })

  it('calls onSelectTopic when a topic is clicked', async () => {
    const onSelectTopic = vi.fn()
    const user = userEvent.setup()
    render(<TopicGrid topics={[topic()]} onSelectTopic={onSelectTopic} />)

    await user.click(screen.getByRole('button', { name: /Arrays/ }))

    expect(onSelectTopic).toHaveBeenCalledWith(expect.objectContaining({ topic_id: 'topic-1' }))
  })
})
