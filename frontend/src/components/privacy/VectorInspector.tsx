interface VectorInspectorProps {
  vector: number[]
  featureNames?: string[]
}

export function VectorInspector({ vector, featureNames }: VectorInspectorProps) {
  const labels = featureNames && featureNames.length === vector.length ? featureNames : null
  return (
    <div className="animate-enter rounded-lg border border-dashed bg-card p-3">
      <p className="eyebrow mb-2">
        Transmitted vector ({vector.length} value{vector.length === 1 ? '' : 's'})
      </p>
      <ul className="grid grid-cols-1 gap-x-4 gap-y-1 font-mono text-xs sm:grid-cols-3 md:grid-cols-4">
        {vector.map((value, index) => (
          <li key={index} className="flex justify-between gap-2">
            <span className="min-w-0 truncate text-muted-foreground">{labels ? labels[index] : `[${index}]`}</span>
            <span>{value.toFixed(6)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
