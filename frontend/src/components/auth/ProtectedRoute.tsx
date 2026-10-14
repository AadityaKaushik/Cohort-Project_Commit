import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { BrandGlyph } from '@/components/brand/BrandMark'
import { RotatingQuote } from '@/components/shared/RotatingQuote'

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
