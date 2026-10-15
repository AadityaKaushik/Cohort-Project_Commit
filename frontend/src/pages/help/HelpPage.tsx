import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { PublicPageFrame } from '@/components/layout/PublicPageFrame'
import { ShortcutList } from '@/components/layout/ShortcutList'
import { SectionCard } from '@/components/shared/SectionCard'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/use-auth'
import { clearTourDone } from '@/lib/tour'

const FAQ = [
  {
    question: 'Who can see my applications?',
    answer: 'Only you. Nothing is shared with other users.',
  },
  {
    question: 'Do I have to join the shared learning?',
    answer: 'No. It is off by default and every feature works without it. You can opt in or out on the Privacy page.',
  },
  {
    question: 'Does Cohort send my text to an AI service?',
    answer: 'Not by default. AI features use a local model. If the operator turns on the optional cloud provider, a notice shows on the feature, and nothing is sent until you press its button.',
  },
  {
    question: 'How do I get my data out?',
    answer: 'Open your profile and use "Download my data". It exports what you entered, including resume files.',
  },
  {
    question: 'How do I delete my account?',
    answer: 'Open your profile and use "Delete account". You will be asked to type DELETE to confirm. It cannot be undone.',
  },
]

export function HelpPage() {
  const { hash } = useLocation()
  const { user } = useAuth()
  const navigate = useNavigate()

  function startTour() {
    if (!user) return
    clearTourDone(user.id)
    navigate('/')
  }

  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView()
    }
  }, [hash])

  return (
    <PublicPageFrame title="Help" description="Short answers, how to get jobs into Cohort, and the keyboard shortcuts.">
      <SectionCard title="Common questions">
        <dl className="divide-y">
          {FAQ.map((item) => (
            <div key={item.question} className="space-y-1 py-3 first:pt-0 last:pb-0">
              <dt className="text-sm font-medium">{item.question}</dt>
              <dd className="text-sm text-pretty text-muted-foreground">{item.answer}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-4 text-sm text-muted-foreground">
          More detail is in the{' '}
          <Link to="/privacy-policy" className="font-medium text-primary underline-offset-4 hover:underline">
            privacy policy
          </Link>
          .
        </p>
      </SectionCard>

      <SectionCard title="Import and bookmark jobs">
        <div className="space-y-3 text-sm text-pretty text-muted-foreground">
          <p>
            <span className="font-medium text-foreground">Import.</span> Go to Applications, then Import. Upload a CSV, check the preview, and confirm. Rows are added to your list.
          </p>
          <p>
            <span className="font-medium text-foreground">Bookmark.</span> On the same Import page, drag the "Save to Cohort" link to your bookmarks bar. On a job page, click it. Cohort opens a new application form with what the page gave it. Check the details and save. The bookmark only reads the page when you click it.
          </p>
        </div>
      </SectionCard>

      <SectionCard title="Keyboard shortcuts" description="Single keys are ignored while you are typing in a field.">
        <ShortcutList />
      </SectionCard>

      <section id="tour" className="scroll-mt-6">
        <SectionCard title="Replay the tour">
          <div className="space-y-4 text-sm text-pretty text-muted-foreground">
            <p>
              The tour walks through each area of the app in order. It can also be started from the user menu, under "Replay product tour".
            </p>
            {user ? (
              <div className="flex flex-wrap items-center gap-3">
                <Button type="button" onClick={startTour}>
                  Start the tour
                </Button>
              </div>
            ) : (
              <p>
                <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                  Log in
                </Link>{' '}
                to take the tour.
              </p>
            )}
          </div>
        </SectionCard>
      </section>
    </PublicPageFrame>
  )
}
