import { TriangleAlertIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface SoftWarningProps {
  id: string
  messages: string[] | undefined
  onEdit: () => void
  onSaveAnyway: () => void
}

export function SoftWarning({ id, messages, onEdit, onSaveAnyway }: SoftWarningProps) {
  if (!messages || messages.length === 0) {
    return null
  }
  return (
    <div
      id={`${id}-warning`}
      role="status"
      className="flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-foreground"
    >
      <TriangleAlertIcon aria-hidden className="size-4 shrink-0 text-amber-600 dark:text-amber-400" />
      <span className="min-w-0 flex-1">{messages.join(' ')}</span>
      <span className="flex gap-2">
        <Button type="button" size="sm" variant="outline" onClick={onEdit}>
          Edit
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onSaveAnyway}>
          Save anyway
        </Button>
      </span>
    </div>
  )
}
