import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeftIcon, CompassIcon } from 'lucide-react'
import { FullPageState } from '@/components/shared/FullPageState'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  useEffect(() => {
    document.title = 'Page not found · Cohort'
  }, [])

  return (
    <FullPageState
      icon={CompassIcon}
      eyebrow="Error 404"
      title="This page doesn't exist"
      description="The link may be broken, or the page may have moved."
      actions={
        <>
          <Button asChild>
            <Link to="/">
              <ArrowLeftIcon aria-hidden />
              Return home
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/help">Open help</Link>
          </Button>
        </>
      }
    />
  )
}
