import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Button } from '@/components/ui/button'

const FEATURES = [
  'Keep a list of your job applications and move them through each stage.',
  'Schedule interviews and write a short debrief after each one.',
  'Plan preparation tasks and practise questions by topic.',
  'See simple charts of how your applications are going.',
]

export function LandingPage() {
  useEffect(() => {
    document.title = 'Cohort'
  }, [])

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <header className="border-b">
        <div className="mx-auto flex h-14 max-w-3xl items-center gap-4 px-4">
          <BrandMark glyphClassName="size-7" wordmarkClassName="text-base" />
          <ThemeToggle className="ml-auto" />
        </div>
      </header>

      <main id="main" className="mx-auto w-full max-w-3xl flex-1 px-4 py-12">
        <h1 className="text-3xl font-semibold">Cohort</h1>
        <p className="mt-2 text-muted-foreground">Job application tracker with interview preparation.</p>

        <ul className="mt-6 list-disc space-y-1 pl-5 text-sm">
          {FEATURES.map((feature) => (
            <li key={feature}>{feature}</li>
          ))}
        </ul>

        <p className="mt-6 text-sm text-muted-foreground">
          Your records are only visible to you. See the{' '}
          <Link to="/privacy-policy" className="underline underline-offset-4 hover:text-foreground">
            privacy policy
          </Link>{' '}
          for details.
        </p>

        <div className="mt-8 flex flex-wrap gap-3">
          <Button asChild>
            <Link to="/login">Log in</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/register">Create account</Link>
          </Button>
        </div>
      </main>

      <footer className="border-t">
        <div className="mx-auto flex max-w-3xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-6 text-sm text-muted-foreground">
          <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-2">
            <Link to="/privacy-policy" className="hover:text-foreground">
              Privacy policy
            </Link>
            <Link to="/help" className="hover:text-foreground">
              Help
            </Link>
          </nav>
          <p className="text-xs sm:ml-auto">© {new Date().getFullYear()} Cohort</p>
        </div>
      </footer>
    </div>
  )
}
