import type { TopicMasteryView } from '@/lib/prep-api'
import { MasteryBadge } from '@/components/prep/MasteryBadge'

export function TopicGrid({ topics, onSelectTopic }: { topics: TopicMasteryView[]; onSelectTopic?: (topic: TopicMasteryView) => void }) {
  if (topics.length === 0) {
    return <p className="rounded-lg border border-dashed bg-surface-2/50 p-6 text-center text-sm text-muted-foreground">No topics yet.</p>
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-6">
      {topics.map((topic) => (
        <button
          key={topic.topic_id}
          type="button"
          onClick={() => onSelectTopic?.(topic)}
          className="flex flex-col items-start justify-between gap-2.5 rounded-lg border bg-card p-3 text-left hover:border-primary/30"
        >
          <span className="text-sm font-medium leading-tight">{topic.topic_name}</span>
          <MasteryBadge
            masteryScore={topic.mastery_score}
            sufficientEvidence={topic.sufficient_evidence}
            observationCount={topic.observation_count}
          />
        </button>
      ))}
    </div>
  )
}
