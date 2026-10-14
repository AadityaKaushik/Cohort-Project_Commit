import { NetworkIcon, UserIcon } from 'lucide-react'
import { Segmented } from '@/components/shared/Segmented'

interface FederatedLiftToggleProps {
  federated: boolean
  onChange: (federated: boolean) => void
  disabled?: boolean
}

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
