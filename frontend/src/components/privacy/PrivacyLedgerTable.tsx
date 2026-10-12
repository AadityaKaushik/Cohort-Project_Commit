import { Fragment, useState } from 'react'
import { ChevronDownIcon, ChevronRightIcon } from 'lucide-react'
import type { PrivacyLedgerEntry } from '@/lib/fl-api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { VectorInspector } from '@/components/privacy/VectorInspector'
import { useIsMobile } from '@/hooks/use-mobile'

interface PrivacyLedgerTableProps {
  entries: PrivacyLedgerEntry[]
  featureNames?: string[]
}

/**
 * US-63: "a log of everything my client has ever sent, so that I can
 * verify the privacy claim instead of trusting it." One immutable row per
 * round; expanding a row reveals the real transmitted vector via
 * {@link VectorInspector} rather than just the summary norms/epsilon.
 */
export function PrivacyLedgerTable({ entries, featureNames }: PrivacyLedgerTableProps) {
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const isMobile = useIsMobile()

  if (isMobile) {
    return (
      <ul className="space-y-2" data-testid="ledger-cards">
        {entries.map((entry) => {
          const expanded = expandedId === entry.id
          return (
            <li key={entry.id} className="rounded-lg border p-3 text-sm">
              <div className="flex items-center justify-between gap-2">
                <div>
                  <p className="font-medium">Round {entry.round_number}</p>
                  <p className="text-xs text-muted-foreground">{new Date(entry.created_at).toLocaleString()}</p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  aria-expanded={expanded}
                  aria-label={`${expanded ? 'Collapse' : 'Expand'} vector for round ${entry.round_number}`}
                  onClick={() => setExpandedId(expanded ? null : entry.id)}
                >
                  {expanded ? <ChevronDownIcon className="h-4 w-4" /> : <ChevronRightIcon className="h-4 w-4" />}
                </Button>
              </div>
              <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-xs">
                <dt className="text-muted-foreground">Cohort size</dt>
                <dd className="text-right">{entry.cohort_size}</dd>
                <dt className="text-muted-foreground">Pre-clip norm</dt>
                <dd className="text-right">{entry.pre_clip_norm.toFixed(4)}</dd>
                <dt className="text-muted-foreground">Post-clip norm</dt>
                <dd className="text-right">{entry.post_clip_norm.toFixed(4)}</dd>
                <dt className="text-muted-foreground">Noise (sigma)</dt>
                <dd className="text-right">{entry.noise_multiplier.toFixed(4)}</dd>
                <dt className="text-muted-foreground">Epsilon</dt>
                <dd className="text-right">{entry.epsilon.toFixed(4)}</dd>
              </dl>
              {expanded && (
                <div className="mt-3">
                  <VectorInspector vector={entry.transmitted_vector} featureNames={featureNames} />
                </div>
              )}
            </li>
          )
        })}
      </ul>
    )
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead />
          <TableHead>Round</TableHead>
          <TableHead>Sent</TableHead>
          <TableHead>Cohort size</TableHead>
          <TableHead>Pre-clip norm</TableHead>
          <TableHead>Post-clip norm</TableHead>
          <TableHead>Noise (sigma)</TableHead>
          <TableHead>Epsilon</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {entries.map((entry) => {
          const expanded = expandedId === entry.id
          return (
            <Fragment key={entry.id}>
              <TableRow className="cursor-pointer" onClick={() => setExpandedId(expanded ? null : entry.id)}>
                <TableCell>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-expanded={expanded}
                    aria-label={`${expanded ? 'Collapse' : 'Expand'} vector for round ${entry.round_number}`}
                  >
                    {expanded ? <ChevronDownIcon className="h-4 w-4" /> : <ChevronRightIcon className="h-4 w-4" />}
                  </Button>
                </TableCell>
                <TableCell>{entry.round_number}</TableCell>
                <TableCell className="text-xs text-muted-foreground">{new Date(entry.created_at).toLocaleString()}</TableCell>
                <TableCell>{entry.cohort_size}</TableCell>
                <TableCell>{entry.pre_clip_norm.toFixed(4)}</TableCell>
                <TableCell>{entry.post_clip_norm.toFixed(4)}</TableCell>
                <TableCell>{entry.noise_multiplier.toFixed(4)}</TableCell>
                <TableCell>{entry.epsilon.toFixed(4)}</TableCell>
              </TableRow>
              {expanded && (
                <TableRow>
                  <TableCell colSpan={8}>
                    <VectorInspector vector={entry.transmitted_vector} featureNames={featureNames} />
                  </TableCell>
                </TableRow>
              )}
            </Fragment>
          )
        })}
      </TableBody>
    </Table>
  )
}
