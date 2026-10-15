import { useId, useState, type KeyboardEvent } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'

interface FaqCategory {
  id: string
  label: string
  items: { question: string; answer: string }[]
}

const FAQ: FaqCategory[] = [
  {
    id: 'getting-started',
    label: 'Getting started',
    items: [
      {
        question: 'What does Cohort do?',
        answer:
          'It keeps your whole job search in one place: applications and their status history, resumes, interviews with debriefs, preparation tasks and practice, analytics, and recommendations based on your own records.',
      },
      {
        question: 'How do I begin?',
        answer:
          'Create an account with your email, or with Google or GitHub where those are enabled. A short onboarding asks for your target role and date and sets up a starter preparation plan.',
      },
      {
        question: 'Is there a quick way to move around?',
        answer: 'Yes. Press Ctrl K (or Cmd K on a Mac) anywhere in the app to open the command palette and jump to any page.',
      },
    ],
  },
  {
    id: 'privacy-ai',
    label: 'Privacy and AI',
    items: [
      {
        question: 'Who can see my data?',
        answer:
          'Only you. Every record belongs to your account, and requests for another person’s data are answered as if it does not exist.',
      },
      {
        question: 'What is federated learning, in plain terms?',
        answer:
          'Instead of collecting everyone’s data in one place, the shared model learns from small summaries computed per person. Only those summaries are combined, never the records themselves.',
      },
      {
        question: 'What leaves my account if I opt in?',
        answer:
          'A short list of capped numbers, such as an average mastery score, with random noise added first. Never resumes, notes, company names or anything you typed. It is off by default, and the Privacy Ledger lists every contribution.',
      },
      {
        question: 'Can I use Cohort without joining the cohort learning?',
        answer:
          'Yes. Federated learning is opt-in and off by default. Every feature works without it, and you can opt out at any time from the Privacy page.',
      },
      {
        question: 'Does Cohort use AI?',
        answer:
          'Recommendations come from transparent rules over your own data, and each one shows the evidence behind it. An AI provider layer is in place: a local model by default, with an optional cloud provider that shows a visible notice. The first AI features are the resume-to-job match, the interview prep pack and email drafts, shipping as they are finished.',
      },
      {
        question: 'Will AI see my data?',
        answer:
          'AI assistance is designed to run on a local model on the Cohort server by default, so your data stays on that server. If an optional hosted provider is ever switched on, the app will say so clearly whenever it is active.',
      },
    ],
  },
  {
    id: 'account',
    label: 'Account and data',
    items: [
      {
        question: 'Can I export my data?',
        answer:
          'Yes. Your profile page downloads a full copy of your data, and the Reports page produces summaries of your applications, interviews and preparation.',
      },
      {
        question: 'What happens to my data if I delete my account?',
        answer:
          'Deleting your account removes your records, your uploaded resume files, any generated report files and the learning features prepared from your account. Download your data first if you want to keep a copy.',
      },
      {
        question: 'Can I change my mind about sharing?',
        answer: 'At any time. Switching federated learning off stops future contributions immediately.',
      },
      {
        question: 'I forgot my password.',
        answer: 'Use “Forgot your password?” on the log-in page and we will email you a reset link.',
      },
    ],
  },
]

export function FaqTabs() {
  const baseId = useId()
  const [active, setActive] = useState(0)
  const category = FAQ[active]

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    const delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0
    if (delta !== 0) {
      event.preventDefault()
      const next = (active + delta + FAQ.length) % FAQ.length
      setActive(next)
      document.getElementById(`${baseId}-tab-${next}`)?.focus()
    }
  }

  return (
    <div>
      <div role="tablist" aria-label="Question categories" className="flex gap-2 overflow-x-auto pb-1">
        {FAQ.map((item, index) => (
          <button
            key={item.id}
            id={`${baseId}-tab-${index}`}
            role="tab"
            type="button"
            aria-selected={index === active}
            aria-controls={`${baseId}-panel`}
            tabIndex={index === active ? 0 : -1}
            onClick={() => setActive(index)}
            onKeyDown={handleKeyDown}
            className={cn(
              'shrink-0 rounded-full border px-4 py-1.5 text-sm transition-colors',
              index === active ? 'border-primary/40 bg-accent font-medium text-foreground' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {item.label}
          </button>
        ))}
      </div>
      <div
        key={category.id}
        id={`${baseId}-panel`}
        role="tabpanel"
        aria-labelledby={`${baseId}-tab-${active}`}
        className="animate-enter mt-6 divide-y rounded-2xl border bg-card"
      >
        {category.items.map((item) => (
          <details key={item.question} className="group px-5">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 font-medium [&::-webkit-details-marker]:hidden">
              {item.question}
              <ChevronDown aria-hidden className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-open:rotate-180" />
            </summary>
            <p className="pb-5 text-sm text-muted-foreground">{item.answer}</p>
          </details>
        ))}
      </div>
    </div>
  )
}
