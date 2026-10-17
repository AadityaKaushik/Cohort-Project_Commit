import { useEffect, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRightIcon } from 'lucide-react'
import { BrandMark } from '@/components/brand/BrandMark'
import { ClimbDemo } from '@/components/landing/ClimbDemo'
import { Faq } from '@/components/landing/Faq'
import { PrivacyFlow } from '@/components/landing/PrivacyFlow'
import { ProblemShowcase } from '@/components/landing/ProblemShowcase'
import { StageExplorer } from '@/components/landing/StageExplorer'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Button } from '@/components/ui/button'
import { useInView } from '@/lib/use-in-view'
import { cn } from '@/lib/utils'

const SECTIONS = [
  { id: 'problems', label: 'Problems' },
  { id: 'how', label: 'How it works' },
  { id: 'privacy', label: 'Privacy' },
  { id: 'faq', label: 'FAQ' },
]

const FIGURES = [
  { value: '6', label: 'stages, Saved to Offer' },
  { value: '288', label: 'original questions' },
  { value: '48', label: 'topics with mastery that fades' },
  { value: '3 min', label: 'to debrief a round' },
]

const PRIVACY_POINTS = [
  'Your applications, notes and resumes never go to the shared model.',
  'Each round sends 24 numbers, clipped and noised first.',
  'Rounds with fewer than 3 people are refused.',
  'The Privacy page lists every round you took part in and what was sent.',
]

const NAV_LINK = 'hover:underline hover:decoration-signal hover:decoration-2 hover:underline-offset-4'

export function LandingPage() {
  useEffect(() => {
    document.title = 'Cohort'
  }, [])

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b-2 border-rule bg-background">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:px-6">
          <BrandMark />
          <nav aria-label="Sections" className="ml-auto hidden items-center gap-5 text-sm font-semibold md:flex">
            {SECTIONS.map((section) => (
              <a key={section.id} href={`#${section.id}`} className={NAV_LINK}>
                {section.label}
              </a>
            ))}
          </nav>
          <ThemeToggle className="ml-auto md:ml-2" />
          <Button variant="outline" asChild className="hidden sm:inline-flex">
            <Link to="/login">Log in</Link>
          </Button>
        </div>
      </header>

      <main id="main" className="flex-1">
        <section className="overflow-hidden bg-brand text-brand-foreground">
          <div className="mx-auto grid max-w-6xl gap-10 px-4 py-14 sm:px-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] lg:items-center lg:py-20">
            <div className="motion-safe:animate-[rise-in_600ms_ease-out_both]">
              <p className="board-label opacity-80">Job application tracker with interview preparation</p>
              <h1 className="text-display mt-4 text-[3.4rem] sm:text-[5.2rem] lg:text-[6rem]">
                From saved <span className="text-signal">to signed.</span>
              </h1>
              <p className="mt-6 max-w-xl text-base/relaxed opacity-90 sm:text-lg/relaxed">
                Cohort follows each application from saved to offer, flags the ones that stall, and turns every interview into practice for the next one.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Button variant="cta" size="xl" asChild>
                  <Link to="/register">
                    Create account
                    <ArrowRightIcon aria-hidden />
                  </Link>
                </Button>
                <Button size="xl" variant="outline" asChild className="border-current/40 text-current hover:border-current hover:text-current">
                  <a href="#problems">See what it fixes</a>
                </Button>
              </div>
            </div>
            <div className="motion-safe:animate-[rise-in_700ms_ease-out_150ms_both]">
              <ClimbDemo />
            </div>
          </div>
          <div className="border-t border-brand-foreground/15">
            <dl className="mx-auto grid max-w-6xl grid-cols-2 px-4 sm:px-6 md:grid-cols-4">
              {FIGURES.map((figure, index) => (
                <div key={figure.label} className={cn('flex flex-col-reverse py-5 pr-4', index > 0 && 'md:border-l md:border-brand-foreground/15 md:pl-6')}>
                  <dt className="mt-1 text-xs opacity-80">{figure.label}</dt>
                  <dd className="text-display text-4xl text-signal">{figure.value}</dd>
                </div>
              ))}
            </dl>
          </div>
        </section>

        <Section
          id="problems"
          eyebrow="The problem"
          title="Three things go wrong in every job search"
          intro="A tracker that is only a status board fixes none of them. Each one below has a part of Cohort built for it."
        >
          <ProblemShowcase />
        </Section>

        <Section
          id="how"
          eyebrow="How it works"
          title="Every stage is a step up"
          intro="Pick a stage to see what Cohort does there. The same steps appear on your dashboard, filled with your own applications."
        >
          <StageExplorer />
        </Section>

        <section id="privacy" aria-labelledby="privacy-heading" className="scroll-mt-16 bg-brand text-brand-foreground">
          <div className="mx-auto grid max-w-6xl gap-10 px-4 py-16 sm:px-6 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] lg:items-center lg:py-20">
            <div>
              <p className="board-label opacity-80">Privacy</p>
              <h2 id="privacy-heading" className="text-display mt-3 text-4xl sm:text-5xl">
                Learns from everyone. <span className="text-signal">Sees no one.</span>
              </h2>
              <p className="mt-5 text-sm/relaxed opacity-90">
                The shared model gets better from many people's preparation without anyone's applications leaving their account. It is off until you turn it on.
              </p>
              <ul className="mt-6 space-y-3 text-sm">
                {PRIVACY_POINTS.map((point) => (
                  <li key={point} className="flex gap-3">
                    <span aria-hidden className="mt-1.5 size-2 shrink-0 rounded-[1px] bg-signal" />
                    {point}
                  </li>
                ))}
              </ul>
            </div>
            <PrivacyFlow />
          </div>
        </section>

        <Section id="faq" eyebrow="Questions" title="Asked before signing up">
          <Faq />
        </Section>

        <ClosingCall />
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

function Section({ id, eyebrow, title, intro, children }: { id: string; eyebrow: string; title: string; intro?: string; children: ReactNode }) {
  const { ref, inView } = useInView<HTMLDivElement>(0.1)
  return (
    <section id={id} aria-labelledby={`${id}-heading`} className="mx-auto w-full max-w-6xl scroll-mt-16 px-4 py-16 sm:px-6 lg:py-20">
      <div
        ref={ref}
        className={cn('transition-[opacity,translate] duration-700 ease-out', inView ? 'translate-y-0 opacity-100' : 'translate-y-6 opacity-0')}
      >
        <div className="mb-10 flex flex-col gap-3 border-b-2 border-rule pb-5 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="board-label text-muted-foreground">{eyebrow}</p>
            <h2 id={`${id}-heading`} className="text-display mt-2 text-4xl sm:text-5xl">
              {title}
            </h2>
          </div>
          {intro && <p className="max-w-md text-sm text-muted-foreground">{intro}</p>}
        </div>
        {children}
      </div>
    </section>
  )
}

function ClosingCall() {
  return (
    <section aria-labelledby="start-heading" className="border-t-2 border-rule bg-signal text-signal-foreground">
      <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-14 sm:px-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 id="start-heading" className="text-display text-5xl sm:text-6xl">
            Start the climb.
          </h2>
          <p className="mt-3 max-w-lg text-sm/relaxed">Log your first application in under a minute, or load sample data and look around first.</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Button size="xl" asChild className="bg-signal-foreground text-signal hover:bg-signal-foreground/90">
            <Link to="/register">
              Create an account
              <ArrowRightIcon aria-hidden />
            </Link>
          </Button>
          <Button size="xl" variant="outline" asChild className="border-signal-foreground/40 text-signal-foreground hover:border-signal-foreground hover:text-signal-foreground">
            <Link to="/login">Log in</Link>
          </Button>
        </div>
      </div>
    </section>
  )
}
