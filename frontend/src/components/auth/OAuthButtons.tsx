import { useEffect, useState } from 'react'
import { API_BASE_URL } from '@/lib/api-client'
import { listOAuthProviders, type OAuthProvider } from '@/lib/auth-api'
import { Button } from '@/components/ui/button'

const PROVIDER_LABELS: Record<OAuthProvider, string> = {
  google: 'Continue with Google',
  github: 'Continue with GitHub',
}

function authorizationUrl(provider: OAuthProvider): string {
  return `${new URL(API_BASE_URL).origin}/oauth2/authorization/${provider}`
}

/** Shows a button only for providers the server reports as configured; renders nothing otherwise. */
export function OAuthButtons() {
  const [providers, setProviders] = useState<OAuthProvider[]>([])

  useEffect(() => {
    let cancelled = false
    listOAuthProviders()
      .then((result) => {
        if (!cancelled) {
          setProviders(result)
        }
      })
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [])

  if (providers.length === 0) {
    return null
  }

  return (
    <div className="mb-4 space-y-2">
      {providers.map((provider) => (
        <Button key={provider} variant="outline" className="w-full" asChild>
          <a href={authorizationUrl(provider)}>{PROVIDER_LABELS[provider]}</a>
        </Button>
      ))}
      <div className="flex items-center gap-3 pt-1 text-xs text-muted-foreground">
        <span className="h-px flex-1 bg-border" />
        or
        <span className="h-px flex-1 bg-border" />
      </div>
    </div>
  )
}
