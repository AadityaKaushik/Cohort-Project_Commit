import { PolicyContent } from '@/components/privacy/PolicyContent'
import { PublicPageFrame } from '@/components/layout/PublicPageFrame'

export function PrivacyPolicyPage() {
  return (
    <PublicPageFrame title="Privacy policy" description="What Cohort stores, who can see it and what leaves your account.">
      <PolicyContent />
    </PublicPageFrame>
  )
}
