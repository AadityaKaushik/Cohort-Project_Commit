import { useEffect, useState } from 'react'
import { API_BASE_URL } from '@/lib/api-client'
import { listOAuthProviders, type OAuthProvider } from '@/lib/auth-api'
import { Button } from '@/components/ui/button'

const PROVIDER_LABELS: Record<OAuthProvider, string> = {
  google: 'Continue with Google',
  github: 'Continue with GitHub',
  linkedin: 'Continue with LinkedIn',
}

function ProviderIcon({ provider }: { provider: OAuthProvider }) {
  if (provider === 'google') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden className="size-4">
        <path fill="#4285F4" d="M22.6 12.3c0-.8-.1-1.5-.2-2.2H12v4.2h5.9a5 5 0 0 1-2.2 3.3v2.7h3.6c2.1-1.9 3.3-4.8 3.3-8z" />
        <path fill="#34A853" d="M12 23c3 0 5.5-1 7.3-2.7l-3.6-2.7c-1 .7-2.2 1.1-3.7 1.1-2.9 0-5.3-1.9-6.2-4.5H2.1v2.8A11 11 0 0 0 12 23z" />
        <path fill="#FBBC05" d="M5.8 14.2a6.6 6.6 0 0 1 0-4.3V7.1H2.1a11 11 0 0 0 0 9.9l3.7-2.8z" />
        <path fill="#EA4335" d="M12 5.4c1.6 0 3.1.6 4.2 1.7l3.2-3.2A11 11 0 0 0 2.1 7.1l3.7 2.8C6.7 7.3 9.1 5.4 12 5.4z" />
      </svg>
    )
  }
  if (provider === 'linkedin') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden className="size-4">
        <rect width="24" height="24" rx="3" fill="#0A66C2" />
        <path
          fill="#fff"
          d="M7.1 9.5h-2.6V19h2.6V9.5zM5.8 5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zM19.5 13.5c0-2.6-1.4-4.2-3.7-4.2-1.3 0-2.2.7-2.6 1.4V9.5h-2.5V19h2.6v-5c0-1.3.6-2.2 1.7-2.2s1.6.8 1.6 2.2v5h2.6l.3-5.5z"
        />
      </svg>
    )
  }
  return (
    <svg viewBox="0 0 24 24" aria-hidden className="size-4" fill="currentColor">
      <path d="M12 .5a11.5 11.5 0 0 0-3.6 22.4c.6.1.8-.3.8-.6v-2c-3.2.7-3.9-1.5-3.9-1.5-.5-1.3-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.7-1.6-2.6-.3-5.3-1.3-5.3-5.7 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.2 1.2a11 11 0 0 1 5.8 0c2.2-1.5 3.2-1.2 3.2-1.2.6 1.6.2 2.8.1 3.1.8.8 1.2 1.8 1.2 3.1 0 4.4-2.7 5.4-5.3 5.7.4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6A11.5 11.5 0 0 0 12 .5z" />
    </svg>
  )
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
    <div className="mb-6 space-y-2.5">
      {providers.map((provider) => (
        <Button key={provider} variant="outline" size="xl" className="w-full" asChild>
          <a href={authorizationUrl(provider)}>
            <ProviderIcon provider={provider} />
            {PROVIDER_LABELS[provider]}
          </a>
        </Button>
      ))}
      <div className="flex items-center gap-3 pt-2 text-xs text-muted-foreground">
        <span className="h-px flex-1 bg-border" />
        or
        <span className="h-px flex-1 bg-border" />
      </div>
    </div>
  )
}
