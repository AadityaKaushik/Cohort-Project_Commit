import { Link } from 'react-router-dom'
import { SectionCard } from '@/components/shared/SectionCard'

export function PolicyContent() {
  return (
    <div className="space-y-4">
      <SectionCard title="What is stored">
        <ul className="list-disc space-y-1.5 pl-5 text-sm text-muted-foreground">
          <li>Your account: email, display name and a hashed password. If you sign in with Google or GitHub, the provider's account id is linked to your account.</li>
          <li>What you enter: applications and their status history, companies, contacts, interviews and debriefs, offers, goals, preparation tasks and practice attempts.</li>
          <li>Files you upload, such as resume files.</li>
          <li>Notifications and report exports created for you.</li>
        </ul>
      </SectionCard>

      <SectionCard title="Who can see it">
        <p className="text-sm text-pretty text-muted-foreground">
          Only you. Every record belongs to one account, and a request for another person's record is answered as if it does not exist. Other users never see your applications, notes or files.
        </p>
      </SectionCard>

      <SectionCard title="How federated learning works">
        <div className="space-y-2 text-sm text-pretty text-muted-foreground">
          <p>
            Cohort can learn from many people without collecting their records in one place. Each account computes a short list of bounded numbers from its own activity, such as an average mastery score. Only those numbers take part in the shared model.
          </p>
          <p>
            It is off by default. If you opt in, the numbers are capped to a fixed range and have random noise added before they leave your account. Resumes, notes, company names and anything you typed are never part of it. You can withdraw at any time; withdrawal applies from the next round, and numbers already combined cannot be taken back out.
          </p>
        </div>
      </SectionCard>

      <SectionCard title="The Privacy Ledger">
        <p className="text-sm text-pretty text-muted-foreground">
          The ledger lists every contribution your account has made, one row per round, including the exact vector that was sent after capping and noise. It is on the{' '}
          <Link to="/privacy" className="font-medium text-primary underline-offset-4 hover:underline">
            Privacy page
          </Link>{' '}
          once you are signed in.
        </p>
      </SectionCard>

      <SectionCard title="Export and delete">
        <p className="text-sm text-pretty text-muted-foreground">
          From your profile you can download your data as a file, including your resume files, and you can delete your account. Deleting removes your account, applications, resumes, files and federated-learning data. It cannot be undone.
        </p>
      </SectionCard>

      <SectionCard title="AI features">
        <ul className="list-disc space-y-1.5 pl-5 text-sm text-muted-foreground">
          <li>By default AI features run on a model on the same machine as Cohort, so text is not sent to an outside service.</li>
          <li>The operator can switch on an optional cloud provider (Gemini). While it is active, a visible notice appears on the AI features, and text from that page is sent to Google to produce the answer.</li>
          <li>Nothing is sent automatically. An AI feature only runs when you press its button.</li>
        </ul>
      </SectionCard>
    </div>
  )
}
