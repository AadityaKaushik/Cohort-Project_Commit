import type { TopicMasteryView } from '@/lib/prep-api'
import { MasteryBadge } from '@/components/prep/MasteryBadge'

/**
 * US-41: "a topic grid coloured by mastery with an accessible non-colour
 * indicator; drill-down from category to topic to questions." The colour
 * reinforcement lives entirely in {@link MasteryBadge} (which always pairs
 * it with a percentage and a plain-language band); this component is just
 * the grid layout plus the click-through hook.
 */
export function TopicGrid({ topics, onSelectTopic }: { topics: TopicMasteryView[]; onSelectTopic?: (topic: TopicMasteryView) => void }) {
  if (topics.length === 0) {
    return <p className="text-sm text-muted-foreground">No topics yet.</p>
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
      {topics.map((topic) => (
        <button
          key={topic.topic_id}
          type="button"
          onClick={() => onSelectTopic?.(topic)}
          className="flex flex-col items-start gap-1.5 rounded-lg border p-3 text-left transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
