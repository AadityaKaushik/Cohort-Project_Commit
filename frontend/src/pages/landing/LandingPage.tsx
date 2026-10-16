import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRightIcon } from 'lucide-react'
import { BrandMark } from '@/components/brand/BrandMark'
import { FlapText } from '@/components/board/FlapText'
import { RouteLine, type RouteStation } from '@/components/board/RouteLine'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Button } from '@/components/ui/button'

const EXAMPLE_ROUTE: RouteStation[] = [
  { key: 'saved', label: 'Saved', count: 9, line: 'saved' },
  { key: 'applied', label: 'Applied', count: 14, line: 'applied' },
  { key: 'oa', label: 'OA', count: 6, line: 'oa' },
  { key: 'interview', label: 'Interview', count: 4, line: 'interview' },
  { key: 'final', label: 'Final', count: 2, line: 'final' },
  { key: 'offer', label: 'Offer', count: 1, line: 'offer' },
]

const EXAMPLE_DEPARTURES = [
  { date: '17 OCT', time: '10:30', company: 'Kestrel Labs', round: 'Technical, R2' },
  { date: '18 OCT', time: '14:00', company: 'Northwind', round: 'HR, R1' },
  { date: '21 OCT', time: '11:15', company: 'Arcadia Bank', round: 'Final' },
]

const TIMETABLE = [
  {
    code: 'Track',
    title: 'Every application on a board',
    text: 'Log a role in seconds, drag it between stages, and see which applications have gone quiet for longer than your usual.',
  },
  {
    code: 'Prepare',
    title: 'Practice that keeps score',
    text: 'Questions by topic, with mastery that fades if you stop. A review queue tells you what to revisit, and a timed mock interview uses your weakest topics.',
  },
  {
    code: 'Debrief',
    title: 'Three minutes after each round',
    text: 'Record the outcome and what you were asked while it is fresh. Any question can go straight into your own question bank.',
  },
  {
    code: 'Review',
    title: 'Numbers that show their sample size',
    text: 'Funnel, conversion and rejection figures, flagged when there is not enough data yet, and suggestions that show the evidence behind them.',
  },
]

export function LandingPage() {
  useEffect(() => {
    document.title = 'Cohort'
  }, [])

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <header className="border-b-2 border-rule">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:px-6">
          <BrandMark />
          <nav aria-label="Site" className="ml-auto hidden items-center gap-5 text-sm font-semibold sm:flex">
            <Link to="/help" className="hover:underline hover:decoration-signal hover:decoration-2 hover:underline-offset-4">
              Help
            </Link>
            <Link to="/privacy-policy" className="hover:underline hover:decoration-signal hover:decoration-2 hover:underline-offset-4">
              Privacy
            </Link>
          </nav>
          <ThemeToggle className="ml-auto sm:ml-2" />
          <Button variant="outline" asChild className="hidden sm:inline-flex">
            <Link to="/login">Log in</Link>
          </Button>
        </div>
      </header>

      <main id="main" className="flex-1">
        <section className="bg-primary text-primary-foreground">
          <div className="mx-auto grid max-w-6xl gap-10 px-4 py-14 sm:px-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)] lg:items-center lg:py-20">
            <div>
              <p className="board-label opacity-80">Job application tracker with interview preparation</p>
              <h1 className="text-display mt-4 text-[3.4rem] sm:text-[5.2rem] lg:text-[6rem]">Every application on one line.</h1>
              <p className="mt-6 max-w-xl text-base/relaxed opacity-90 sm:text-lg/relaxed">
                Cohort follows each application from saved to offer, flags the ones that stall, and turns every interview into practice for the next one.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Button variant="cta" size="xl" asChild className="dark:bg-primary-foreground dark:text-primary dark:hover:bg-primary-foreground/85">
                  <Link to="/register">
                    Create account
                    <ArrowRightIcon aria-hidden />
                  </Link>
                </Button>
                <Button
                  size="xl"
                  variant="outline"
                  asChild
                  className="border-current/40 text-current hover:border-current hover:text-current sm:hidden"
                >
                  <Link to="/login">Log in</Link>
                </Button>
              </div>
            </div>

            <figure aria-label="Example of the interview board" className="rounded-md border-2 border-current/25 bg-background text-foreground">
              <div className="flex items-center justify-between border-b px-4 py-2.5">
                <span className="text-display text-xl">Next interviews</span>
                <span className="board-label text-muted-foreground">Example</span>
              </div>
              <ul className="divide-y">
                {EXAMPLE_DEPARTURES.map((row) => (
                  <li key={row.company} className="grid grid-cols-[4.2rem_3.2rem_minmax(0,1fr)] items-center gap-3 px-4 py-3">
                    <FlapText value={row.date} className="text-sm font-semibold" />
                    <FlapText value={row.time} className="text-sm text-muted-foreground" />
                    <span className="min-w-0">
                      <span className="text-display block truncate text-lg">{row.company}</span>
                      <span className="block truncate text-xs text-muted-foreground">{row.round}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </figure>
          </div>
        </section>

        <section aria-labelledby="route-heading" className="mx-auto max-w-6xl px-4 py-14 sm:px-6">
          <div className="flex flex-col gap-2 border-b-2 border-rule pb-4 sm:flex-row sm:items-end sm:justify-between">
            <h2 id="route-heading" className="text-display text-4xl">
              The line
            </h2>
            <p className="max-w-md text-sm text-muted-foreground">
              Six stations, one colour each. Every application sits on one of them, and the counts tell you where your search stands.
            </p>
          </div>
          <div className="mt-8 rounded-md border bg-card px-4 py-8 sm:px-8">
            <RouteLine stations={EXAMPLE_ROUTE} />
          </div>
        </section>

        <section aria-labelledby="timetable-heading" className="mx-auto max-w-6xl px-4 pb-16 sm:px-6">
          <h2 id="timetable-heading" className="text-display border-b-2 border-rule pb-4 text-4xl">
            What you do in Cohort
          </h2>
          <ol>
            {TIMETABLE.map((row, index) => (
              <li key={row.code} className="grid gap-2 border-b py-6 sm:grid-cols-[3rem_9rem_minmax(0,1fr)] sm:gap-6">
                <span className="tabular text-sm text-muted-foreground">{String(index + 1).padStart(2, '0')}</span>
                <span className="board-label self-start justify-self-start rounded-[3px] bg-signal px-2 py-1 text-signal-foreground">{row.code}</span>
                <div>
                  <h3 className="text-display text-2xl">{row.title}</h3>
                  <p className="mt-2 max-w-2xl text-sm/relaxed text-muted-foreground">{row.text}</p>
                </div>
              </li>
            ))}
          </ol>
          <p className="mt-8 max-w-2xl text-sm/relaxed text-muted-foreground">
            Your records stay in your account and nobody else can see them. The optional shared model only receives clipped, noised numbers, and every round
            is listed on your Privacy page. Read the{' '}
            <Link to="/privacy-policy" className="font-semibold text-foreground underline decoration-signal decoration-2 underline-offset-4">
              privacy policy
            </Link>
            .
          </p>
        </section>
      </main>

      <footer className="border-t-2 border-rule">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-6 text-sm text-muted-foreground sm:px-6">
          <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-2">
            <Link to="/privacy-policy" className="hover:text-foreground">
              Privacy policy
            </Link>
            <Link to="/help" className="hover:text-foreground">
              Help
            </Link>
            <Link to="/login" className="hover:text-foreground">
              Log in
            </Link>
          </nav>
          <p className="text-xs sm:ml-auto">© {new Date().getFullYear()} Cohort</p>
        </div>
      </footer>
    </div>
  )
}
