import { NetworkIcon, UserIcon } from 'lucide-react'
import { Segmented } from '@/components/shared/Segmented'

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
    <Segmented
      label="Federated Lift toggle"
      value={federated ? 'federated' : 'local'}
      onChange={(value) => onChange(value === 'federated')}
      disabled={disabled}
      options={[
        { value: 'local', label: 'Local only', icon: UserIcon },
        { value: 'federated', label: 'Federated lift', icon: NetworkIcon },
      ]}
    />
  )
}
