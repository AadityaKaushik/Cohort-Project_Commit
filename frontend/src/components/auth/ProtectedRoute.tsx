import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { BrandGlyph } from '@/components/brand/BrandMark'
import { RotatingQuote } from '@/components/shared/RotatingQuote'

/** Gate for any route tree that requires a logged-in user (US-01..04).
 * Renders a loading placeholder while the session is being resolved (token
 * present but profile not yet fetched/refreshed). A signed-out visitor at the
 * site root sees the public home page; anywhere else they are sent to /login,
 * preserving the attempted location so login can return there. */
export function ProtectedRoute({ signedOutHome }: { signedOutHome?: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div
        className="flex h-screen flex-col items-center justify-center gap-6 bg-background px-6"
        role="status"
        aria-label="Checking session"
      >
        <BrandGlyph animated className="size-11" />
        <RotatingQuote rotate={false} className="max-w-xs text-sm" />
      </div>
    )
  }

  if (!isAuthenticated) {
    if (signedOutHome && location.pathname === '/') {
      return <>{signedOutHome}</>
    }
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
