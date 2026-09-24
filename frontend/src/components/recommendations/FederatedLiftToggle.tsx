import { NetworkIcon, UserIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface FederatedLiftToggleProps {
  federated: boolean
  onChange: (federated: boolean) => void
  disabled?: boolean
}

/**
 * US-65: "compare my recommendations with and without the shared model."
 * A live toggle, not a static comparison page -- switching it re-fetches
 * {@code GET /recommendations?federated=...} and re-renders in place, so
 * changed items (via {@code rank_delta}) are visibly the same list,
 * reordered, not a second unrelated list.
 */
export function FederatedLiftToggle({ federated, onChange, disabled }: FederatedLiftToggleProps) {
  return (
    <div className="flex items-center gap-1 rounded-md border p-1" role="group" aria-label="Federated Lift toggle">
      <Button
        type="button"
        size="sm"
        variant={federated ? 'ghost' : 'secondary'}
        disabled={disabled}
        aria-pressed={!federated}
        onClick={() => onChange(false)}
      >
        <UserIcon className="h-4 w-4" />
        Local only
      </Button>
      <Button
        type="button"
        size="sm"
        variant={federated ? 'secondary' : 'ghost'}
        disabled={disabled}
        aria-pressed={federated}
        onClick={() => onChange(true)}
      >
        <NetworkIcon className="h-4 w-4" />
        Federated lift
      </Button>
    </div>
  )
}
