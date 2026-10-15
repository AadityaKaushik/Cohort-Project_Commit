interface DifficultyBadgeProps {
  level: number | null
}

export const DIFFICULTY_LABEL = 'Indicative, from reported descriptions'

export function DifficultyBadge({ level }: DifficultyBadgeProps) {
  if (level === null) {
    return <span className="text-xs text-muted-foreground">No sourced rating</span>
  }
  return (
    <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
      <span aria-hidden className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map((dot) => (
          <span key={dot} className={`size-1.5 rounded-full ${dot <= level ? 'bg-primary' : 'bg-muted-foreground/30'}`} />
        ))}
      </span>
      <span>Difficulty {level}/5</span>
      <span className="sr-only">. {DIFFICULTY_LABEL}</span>
    </span>
  )
}
