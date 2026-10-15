import { useState } from 'react'
import { CalendarPlusIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useToast } from '@/hooks/use-toast'

interface CalendarDownloadButtonProps {
  label: string
  download: () => Promise<void>
  variant?: 'outline' | 'ghost' | 'link'
  size?: 'default' | 'sm'
  className?: string
  stopPropagation?: boolean
}

export function CalendarDownloadButton({
  label,
  download,
  variant = 'outline',
  size = 'default',
  className,
  stopPropagation = false,
}: CalendarDownloadButtonProps) {
  const { toast } = useToast()
  const [busy, setBusy] = useState(false)

  async function handleClick(event: React.MouseEvent) {
    if (stopPropagation) event.stopPropagation()
    setBusy(true)
    try {
      await download()
    } catch {
      toast({ title: 'Could not download the calendar file', description: 'Try again in a moment.', variant: 'error' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <Button variant={variant} size={size} className={className} onClick={handleClick} disabled={busy}>
      <CalendarPlusIcon aria-hidden />
      {label}
    </Button>
  )
}
