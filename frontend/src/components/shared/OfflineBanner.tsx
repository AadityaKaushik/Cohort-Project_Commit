import { WifiOffIcon } from 'lucide-react'
import { useOnlineStatus } from '@/hooks/use-online-status'

export function OfflineBanner() {
  const online = useOnlineStatus()
  if (online) {
    return null
  }
  return (
    <div
      role="status"
      className="animate-enter fixed inset-x-0 top-0 z-[60] flex items-center justify-center gap-2 bg-foreground px-4 py-2 text-sm text-background"
    >
      <WifiOffIcon aria-hidden className="size-4 shrink-0" />
      <span>You're offline. Changes can't be saved until the connection is back.</span>
    </div>
  )
}
