import { useEffect, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { PageHeader } from '@/components/shared/PageHeader'
import { useAuth } from '@/hooks/use-auth'

interface PublicPageFrameProps {
  title: string
  description: string
  children: ReactNode
}

export function PublicPageFrame({ title, description, children }: PublicPageFrameProps) {
  const { isAuthenticated } = useAuth()

  useEffect(() => {
    document.title = `${title} · Cohort`
  }, [title])

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex items-center justify-between gap-4 px-5 py-4 sm:px-10">
        <Link to="/" aria-label="Cohort home" className="rounded-lg">
          <BrandMark />
        </Link>
        <ThemeToggle />
      </header>
      <main className="mx-auto w-full max-w-3xl flex-1 space-y-6 px-5 pt-4 pb-16 sm:px-6">
        <PageHeader asH1 title={title} description={description} />
        {children}
      </main>
      <footer className="border-t px-5 py-6 text-sm text-muted-foreground sm:px-10">
        <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-2">
          <Link to="/privacy-policy" className="hover:text-foreground">
            Privacy policy
          </Link>
          <Link to="/help" className="hover:text-foreground">
            Help
          </Link>
          <Link to={isAuthenticated ? '/' : '/login'} className="hover:text-foreground">
            {isAuthenticated ? 'Back to the app' : 'Log in'}
          </Link>
        </nav>
      </footer>
    </div>
  )
}
