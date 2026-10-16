import { Link, type Location } from 'react-router-dom'
import { LogInIcon, TimerOffIcon } from 'lucide-react'
import { FullPageState } from '@/components/shared/FullPageState'
import { Button } from '@/components/ui/button'

export function SessionExpiredScreen({ from }: { from: Location }) {
  return (
    <FullPageState
      icon={TimerOffIcon}
      eyebrow="Signed out"
      title="Your session has ended"
      description="Your session expired. Log in again."
      actions={
        <Button asChild>
          <Link to="/login" state={{ from }}>
            <LogInIcon aria-hidden />
            Sign in again
          </Link>
        </Button>
      }
    />
  )
}
