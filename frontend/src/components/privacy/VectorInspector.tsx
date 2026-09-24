interface VectorInspectorProps {
  vector: number[]
  /** Schema feature names, in order. Used as labels only when their count matches the vector's length. */
  featureNames?: string[]
}

/**
 * US-63: "the exact transmitted vector, expandable." Shows every value in
 * the client's real post-clip-post-noise contribution — not a summary
 * statistic, the actual numbers, labelled with the schema's feature names
 * (or plain indexes if no matching schema is available) so they can be
 * cross-checked against the allow-list on the Privacy page above.
 */
export function VectorInspector({ vector, featureNames }: VectorInspectorProps) {
  const labels = featureNames && featureNames.length === vector.length ? featureNames : null
  return (
    <div className="rounded-md border border-dashed bg-muted/30 p-3">
      <p className="mb-2 text-xs font-medium text-muted-foreground">
        Transmitted vector ({vector.length} value{vector.length === 1 ? '' : 's'})
      </p>
      <ul className="grid grid-cols-2 gap-x-4 gap-y-1 font-mono text-xs sm:grid-cols-3 md:grid-cols-4">
        {vector.map((value, index) => (
          <li key={index} className="flex justify-between gap-2">
            <span className="truncate text-muted-foreground">{labels ? labels[index] : `[${index}]`}</span>
            <span>{value.toFixed(6)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
