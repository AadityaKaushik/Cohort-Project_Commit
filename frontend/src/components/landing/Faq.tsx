import { PlusIcon } from 'lucide-react'

const QUESTIONS: { q: string; a: string }[] = [
  {
    q: 'Who is Cohort for?',
    a: 'Students and early-career job seekers running many applications at once, especially for software roles. The question bank covers data structures and algorithms, system design, databases, operating systems and networking, object-oriented design and behavioural rounds.',
  },
  {
    q: 'Does it cost anything?',
    a: 'No. Create an account with your email, or sign in with Google, GitHub or LinkedIn.',
  },
  {
    q: 'Does Cohort apply to jobs or read my LinkedIn for me?',
    a: 'No. Nothing is scraped and no account is connected. You bring applications in yourself: type them, save them from a job page with the bookmarklet, or import a CSV such as LinkedIn\'s own applications export.',
  },
  {
    q: 'I already track everything in a spreadsheet. Can I move it over?',
    a: 'Yes. The importer reads a CSV, shows a preview of what it found, and maps statuses it does not recognise to Saved with a warning before anything is saved.',
  },
  {
    q: 'Who can see my applications and notes?',
    a: 'Only you. Every request is checked against your account, and asking for someone else\'s record returns "not found". You can export all your data or delete your account at any time.',
  },
  {
    q: 'What is the shared model, and do I have to join it?',
    a: 'It learns from many people\'s preparation patterns without collecting their data. It is off until you turn it on. When it is on, only a clipped, noised set of 24 numbers leaves your account in each round, and the Privacy page lists every round you took part in.',
  },
  {
    q: 'Does the AI read my data?',
    a: 'Only your own, to answer your own request. It runs on a local model by default, so nothing leaves the server. If a hosted provider is turned on, the app tells you while it is active. Drafts are never sent for you.',
  },
  {
    q: 'Why do I need to enter a code when I sign up?',
    a: 'The 6-digit code proves the email address is yours, so nobody can create an account with an address they do not own. The same kind of code resets a forgotten password.',
  },
  {
    q: 'Does it work on my phone?',
    a: 'Yes. Every page works at phone width, in light and dark themes.',
  },
]

export function Faq() {
  return (
    <div className="divide-y border-y-2 border-rule">
      {QUESTIONS.map((item) => (
        <details key={item.q} className="group py-1">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-left text-base font-semibold sm:text-lg [&::-webkit-details-marker]:hidden">
            {item.q}
            <span className="flex size-8 shrink-0 items-center justify-center rounded-sm border border-foreground/25 transition-[transform,background-color,color] duration-200 group-open:rotate-45 group-open:bg-signal group-open:text-signal-foreground">
              <PlusIcon aria-hidden className="size-4" />
            </span>
          </summary>
          <p className="max-w-3xl pb-5 text-sm/relaxed text-muted-foreground">{item.a}</p>
        </details>
      ))}
    </div>
  )
}
