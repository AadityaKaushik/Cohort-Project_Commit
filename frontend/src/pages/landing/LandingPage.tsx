import { useEffect, useState, type CSSProperties, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  BarChart3,
  Briefcase,
  Command,
  Compass,
  EyeOff,
  Lock,
  MessagesSquare,
  PencilLine,
  ShieldCheck,
  Sigma,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { BrandMark } from '@/components/brand/BrandMark'
import { FaqTabs } from '@/components/landing/FaqTabs'
import { CoachCard, CohortCompare, InterviewLoop, MasteryBars, MiniBoard, MiniFunnel } from '@/components/landing/ModuleVisuals'
import { FloatingChips } from '@/components/landing/FloatingChips'
import { HeroBackdrop } from '@/components/landing/HeroBackdrop'
import { JobMatchDemo } from '@/components/landing/JobMatchDemo'
import { JourneyTimeline } from '@/components/landing/JourneyTimeline'
import { PaletteDemo } from '@/components/landing/PaletteDemo'
import { Magnetic } from '@/components/motion/Magnetic'
import { TransitionLink } from '@/components/motion/TransitionLink'
import { ProductPreview } from '@/components/landing/ProductPreview'
import { TypingText } from '@/components/motion/TypingText'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Reveal } from '@/components/motion/Reveal'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

function stagger(index: number): CSSProperties {
  return { animationDelay: `${120 + index * 110}ms` }
}

function useScrolled(threshold = 8): boolean {
  const [scrolled, setScrolled] = useState(false)
  useEffect(() => {
    let frame = 0
    const update = () => {
      frame = 0
      setScrolled(window.scrollY > threshold)
    }
    const handleScroll = () => {
      if (!frame) {
        frame = window.requestAnimationFrame(update)
      }
    }
    update()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => {
      window.cancelAnimationFrame(frame)
      window.removeEventListener('scroll', handleScroll)
    }
  }, [threshold])
  return scrolled
}

const USE_CASES = ['Track every application.', 'Prepare for every interview.', 'Learn from your cohort, privately.'] as const

const NAV_LINKS = [
  { href: '#modules', label: 'Product' },
  { href: '#privacy', label: 'Privacy' },
  { href: '#faq', label: 'FAQ' },
]

function PrimaryActions({ className }: { className?: string }) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 sm:flex-row', className)}>
      <Magnetic className="w-full sm:w-auto">
        <Button variant="cta" size="xl" asChild className="group/cta w-full sm:w-auto">
          <TransitionLink to="/register">
            Create your free account
            <ArrowRight aria-hidden className="transition-transform duration-200 group-hover/cta:translate-x-0.5" />
          </TransitionLink>
        </Button>
      </Magnetic>
      <Magnetic className="w-full sm:w-auto">
        <Button variant="outline" size="xl" asChild className="w-full sm:w-auto">
          <TransitionLink to="/login">Log in</TransitionLink>
        </Button>
      </Magnetic>
    </div>
  )
}

export function LandingPage() {
  const scrolled = useScrolled()

  useEffect(() => {
    document.title = 'Cohort · Job search and interview prep'
  }, [])

  return (
    <div className="min-h-screen bg-background text-foreground">
      <a
        href="#main"
        className="sr-only z-50 rounded-md bg-primary px-3 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Skip to content
      </a>
      <header
        data-scrolled={scrolled}
        className="sticky top-0 z-40 border-b border-transparent transition-[background-color,border-color] duration-300 data-[scrolled=true]:frosted data-[scrolled=true]:border-border"
      >
        <nav aria-label="Primary" className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
          <Link to="/" aria-label="Cohort home" className="rounded-lg">
            <BrandMark animated className="brand-continuity" />
          </Link>
          <div className="ml-6 hidden items-center gap-7 text-sm text-muted-foreground md:flex">
            {NAV_LINKS.map((link) => (
              <a key={link.href} href={link.href} className="transition-colors hover:text-foreground">
                {link.label}
              </a>
            ))}
          </div>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle className="hidden sm:inline-flex" />
            <Button variant="ghost" asChild>
              <TransitionLink to="/login">Log in</TransitionLink>
            </Button>
            <Button asChild className="hidden sm:inline-flex">
              <TransitionLink to="/register">Get started</TransitionLink>
            </Button>
          </div>
        </nav>
      </header>

      <main id="main">
        <section aria-labelledby="hero-title" className="relative isolate overflow-hidden">
          <HeroBackdrop />
          <div className="mx-auto max-w-6xl px-4 pt-16 pb-20 text-center sm:px-6 sm:pt-24 lg:pt-28">
            <p
              className="animate-enter mx-auto inline-flex items-center gap-2 rounded-full border bg-card/70 px-3 py-1 text-xs font-medium text-muted-foreground"
              style={stagger(0)}
            >
              <span className="size-1.5 rounded-full bg-primary" aria-hidden />
              Applications, interview prep and analytics in one place
            </p>
            <h1
              id="hero-title"
              className="animate-enter text-display mx-auto mt-6 max-w-4xl text-[2.6rem] text-balance sm:text-6xl lg:text-[4.75rem]"
              style={stagger(1)}
            >
              Run your job search like a{' '}
              <span className="relative inline-block whitespace-nowrap">
                <span className="relative z-10">real campaign.</span>
                <svg
                  aria-hidden
                  viewBox="0 0 300 20"
                  preserveAspectRatio="none"
                  className="highlight-stroke absolute inset-x-0 -bottom-1 z-0 h-[0.32em] w-full text-primary/45 sm:-bottom-2"
                >
                  <path d="M4 14 C 70 4, 150 4, 296 10" fill="none" stroke="currentColor" strokeWidth="7" strokeLinecap="round" />
                </svg>
              </span>
            </h1>
            <p
              className="animate-enter mt-5 font-heading text-lg font-medium tracking-tight text-primary sm:text-2xl"
              style={stagger(2)}
            >
              <TypingText text={USE_CASES} />
            </p>
            <p
              className="animate-enter mx-auto mt-4 max-w-2xl text-base text-pretty text-muted-foreground sm:text-lg"
              style={stagger(2)}
            >
              Applications, interviews, practice and analytics in one calm workspace, with benchmarks learned
              privately from people on the same path.
            </p>
            <div className="animate-enter mt-9" style={stagger(3)}>
              <PrimaryActions />
            </div>
            <div className="animate-enter mt-16 sm:mt-20" style={stagger(5)}>
              <FloatingChips>
                <ProductPreview />
              </FloatingChips>
            </div>
          </div>
        </section>

        <section id="journey" aria-labelledby="journey-title" className="defer-render scroll-mt-20 border-t">
          <div className="mx-auto grid max-w-6xl gap-12 px-4 py-20 sm:px-6 sm:py-28 lg:grid-cols-[1fr_1.15fr] lg:gap-16">
            <Reveal className="lg:sticky lg:top-28 lg:self-start">
              <p className="text-sm font-medium text-primary">The journey</p>
              <h2 id="journey-title" className="text-display mt-3 text-3xl sm:text-5xl">
                <TypingText text="Four steps, one place." sessionKey="landing-journey-title" />
              </h2>
              <p className="mt-4 text-muted-foreground sm:text-lg">
                Most searches live in a spreadsheet, a notes app and memory. Cohort keeps each step connected, so
                nothing you learn gets lost between them.
              </p>
            </Reveal>
            <JourneyTimeline />
          </div>
        </section>

        <section id="modules" aria-labelledby="modules-title" className="defer-render scroll-mt-20 border-t bg-surface-2/40">
          <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
            <Reveal className="max-w-2xl">
              <p className="text-sm font-medium text-primary">How it works</p>
              <h2 id="modules-title" className="text-display mt-3 text-3xl sm:text-5xl">
                <TypingText text="One loop, from first application to signed offer." sessionKey="landing-modules-title" />
              </h2>
              <p className="mt-4 text-muted-foreground sm:text-lg">
                Applications create interviews, interviews create prep, and prep shows up in your numbers.
              </p>
            </Reveal>

            <div className="mt-12 grid gap-4 md:grid-cols-6">
              <ModuleCard className="md:col-span-4" icon={Briefcase} name="Track" title="A board that knows the rules"
                body="Move roles from saved to offer. Illegal jumps are blocked, corrections need a note, and stalled applications are flagged.">
                <MiniBoard />
              </ModuleCard>
              <ModuleCard className="md:col-span-2" delay={80} icon={PencilLine} name="Prepare" title="Practice that moves mastery"
                body="A question bank across 48 topics. Every logged attempt updates your mastery.">
                <MasteryBars />
              </ModuleCard>
              <ModuleCard className="md:col-span-2" icon={MessagesSquare} name="Interview" title="Every round, debriefed"
                body="Log rounds and the questions you were asked. Weak spots become prep tasks.">
                <InterviewLoop />
              </ModuleCard>
              <ModuleCard className="md:col-span-2" delay={80} icon={BarChart3} name="Analyse" title="Numbers you can trust"
                body="Funnel, conversion and weekly activity, flagged when the sample is too small.">
                <MiniFunnel />
              </ModuleCard>
              <ModuleCard className="md:col-span-2" delay={160} icon={Compass} name="Coach" title="Next steps, with reasons"
                body="Recommendations ranked from your own records, each showing its evidence.">
                <CoachCard />
              </ModuleCard>
              <ModuleCard className="md:col-span-4" icon={Users} name="Cohort Insights" title="Benchmarks without sharing your records"
                body="See how your pace compares with people on the same path. Benchmarks are learned with federated learning, so individual records never leave their accounts.">
                <CohortCompare />
              </ModuleCard>
              <ModuleCard className="md:col-span-2" delay={80} icon={Command} name="Keyboard first" title="Everything, one shortcut away"
                body="Press Ctrl K anywhere to jump to any page.">
                <PaletteDemo />
              </ModuleCard>
            </div>
          </div>
        </section>

        <section id="match" aria-labelledby="match-title" className="defer-render scroll-mt-20 border-t">
          <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
            <Reveal className="max-w-2xl">
              <p className="text-sm font-medium text-primary">Try it</p>
              <h2 id="match-title" className="text-display mt-3 text-3xl sm:text-5xl">
                <TypingText text="How well does a job fit?" sessionKey="landing-match-title" />
              </h2>
              <p className="mt-4 text-muted-foreground sm:text-lg">
                Paste a job description to see which skills a sample resume covers and which it is missing. It runs
                entirely in your browser.
              </p>
            </Reveal>
            <Reveal className="mt-10" delay={80}>
              <JobMatchDemo />
            </Reveal>
          </div>
        </section>

        <section id="privacy" aria-labelledby="privacy-title" className="defer-render scroll-mt-20 border-t">
          <div className="mx-auto grid max-w-6xl gap-12 px-4 py-20 sm:px-6 sm:py-28 lg:grid-cols-[1fr_1.2fr] lg:gap-16">
            <Reveal>
              <p className="text-sm font-medium text-primary">Privacy by design</p>
              <h2 id="privacy-title" className="text-display mt-3 text-3xl sm:text-5xl">
                <TypingText text="The model travels. Your data stays." sessionKey="landing-privacy-title" />
              </h2>
              <p className="mt-4 text-muted-foreground sm:text-lg">
                Federated learning lets a shared model learn from many people without anyone handing over their
                records. Taking part is off by default, and you can switch it off at any time.
              </p>
              <p className="mt-6 flex items-center gap-2 text-sm text-muted-foreground">
                <ShieldCheck aria-hidden className="size-4 text-success" />
                Every contribution is listed in a ledger you can inspect.
              </p>
            </Reveal>
            <ol className="grid gap-4">
              <PrivacyStep index={1} icon={Lock} title="Your records stay in your account"
                body="Resumes, notes, company names and anything you type are never part of the shared model." />
              <PrivacyStep index={2} icon={Sigma} title="Only bounded numbers, with noise"
                body="If you opt in, a short list of capped summary numbers has random noise added before it is sent, so no single person can be picked out." />
              <PrivacyStep index={3} icon={EyeOff} title="Better benchmarks for everyone"
                body="The combined result improves benchmarks and recommendations for people on the same path." />
            </ol>
          </div>
        </section>

        <section id="faq" aria-labelledby="faq-title" className="defer-render scroll-mt-20 border-t bg-surface-2/40">
          <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 sm:py-28">
            <Reveal>
              <h2 id="faq-title" className="text-display text-3xl sm:text-5xl">
                <TypingText text="Questions, answered." sessionKey="landing-faq-title" />
              </h2>
              <div className="mt-8">
                <FaqTabs />
              </div>
            </Reveal>
          </div>
        </section>

        <section aria-labelledby="cta-title" className="defer-render border-t">
          <Reveal className="mx-auto max-w-6xl px-4 py-20 text-center sm:px-6 sm:py-24">
            <h2 id="cta-title" className="text-display mx-auto max-w-3xl text-3xl text-balance sm:text-5xl">
              <TypingText text="Start with one application. See the whole picture." sessionKey="landing-cta-title" />
            </h2>
            <PrimaryActions className="mt-8" />
          </Reveal>
        </section>
      </main>

      <footer className="border-t">
        <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-10 sm:flex-row sm:items-center sm:px-6">
          <BrandMark glyphClassName="size-7" wordmarkClassName="text-base" />
          <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground sm:ml-auto">
            <a href="#privacy" className="hover:text-foreground">
              Privacy
            </a>
            <TransitionLink to="/login" className="hover:text-foreground">
              Log in
            </TransitionLink>
            <TransitionLink to="/register" className="hover:text-foreground">
              Create account
            </TransitionLink>
          </nav>
          <p className="text-xs text-muted-foreground sm:ml-6">© {new Date().getFullYear()} Cohort</p>
        </div>
      </footer>
    </div>
  )
}

interface ModuleCardProps {
  icon: LucideIcon
  name: string
  title: string
  body: string
  className?: string
  delay?: number
  children: ReactNode
}

function ModuleCard({ icon: Icon, name, title, body, className, delay, children }: ModuleCardProps) {
  return (
    <Reveal as="article" delay={delay} className={cn('surface-raised lift flex flex-col overflow-hidden rounded-2xl border', className)}>
      <div className="p-6 pb-0">
        <p className="flex items-center gap-2 text-xs font-medium tracking-wide text-primary uppercase">
          <Icon aria-hidden className="size-3.5" />
          {name}
        </p>
        <h3 className="mt-3 text-lg font-semibold tracking-tight">{title}</h3>
        <p className="mt-1.5 text-sm text-muted-foreground">{body}</p>
      </div>
      <div aria-hidden className="mt-6 flex-1 p-6 pt-0">
        {children}
      </div>
    </Reveal>
  )
}

interface PrivacyStepProps {
  index: number
  icon: LucideIcon
  title: string
  body: string
}

function PrivacyStep({ index, icon: Icon, title, body }: PrivacyStepProps) {
  return (
    <Reveal as="li" delay={index * 90} className="surface-raised flex gap-4 rounded-2xl border p-5">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-surface-2 text-primary">
        <Icon aria-hidden className="size-4" />
      </span>
      <div>
        <h3 className="font-semibold tracking-tight">
          <span className="mr-2 font-mono text-xs text-muted-foreground tabular-nums">0{index}</span>
          {title}
        </h3>
        <p className="mt-1 text-sm text-muted-foreground">{body}</p>
      </div>
    </Reveal>
  )
}
