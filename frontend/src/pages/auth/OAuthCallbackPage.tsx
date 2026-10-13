import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { setTokens } from '@/lib/auth-storage'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'

/** Receives the application's own tokens from the provider round trip and starts a normal session. */
export function OAuthCallbackPage() {
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.replace(/^#/, ''))
    const accessToken = params.get('access_token')
    const refreshToken = params.get('refresh_token')
    if (!accessToken || !refreshToken) {
      setFailed(true)
      return
    }
    setTokens(accessToken, refreshToken)
    window.location.replace('/')
  }, [])

  return (
    <AuthFrame>
      <AuthIntro title="Signing you in" />
      <div className="space-y-4 text-sm">
          {failed ? (
            <p role="alert">
              Sign-in could not be completed.{' '}
              <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                Back to log in
              </Link>
            </p>
          ) : (
            <p role="status">One moment.</p>
          )}
        </div>
    </AuthFrame>
  )
}
