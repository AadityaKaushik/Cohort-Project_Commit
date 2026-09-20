import { Route, Routes } from 'react-router-dom'
import {
  BarChart3,
  LayoutDashboard,
  ShieldCheck,
  Sparkles,
  UserRound,
} from 'lucide-react'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AppShell } from '@/components/layout/AppShell'
import { ApplicationDetailPage } from '@/pages/applications/ApplicationDetailPage'
import { ApplicationFormPage } from '@/pages/applications/ApplicationFormPage'
import { ApplicationsListPage } from '@/pages/applications/ApplicationsListPage'
import { ResumeDetailPage } from '@/pages/resumes/ResumeDetailPage'
import { ResumeFormPage } from '@/pages/resumes/ResumeFormPage'
import { ResumesListPage } from '@/pages/resumes/ResumesListPage'
import { InterviewDebriefPage } from '@/pages/interviews/InterviewDebriefPage'
import { InterviewDetailPage } from '@/pages/interviews/InterviewDetailPage'
import { InterviewFormPage } from '@/pages/interviews/InterviewFormPage'
import { InterviewsListPage } from '@/pages/interviews/InterviewsListPage'
import { PrepPage } from '@/pages/prep/PrepPage'
import { ComingSoonPage } from '@/pages/ComingSoonPage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegisterPage } from '@/pages/RegisterPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route
            index
            element={
              <ComingSoonPage
                icon={LayoutDashboard}
                title="Your dashboard is on its way"
                description="Once applications and interviews start rolling in, this is where you'll see today's action queue and pipeline overview."
              />
            }
          />
          <Route path="applications">
            <Route index element={<ApplicationsListPage />} />
            <Route path="new" element={<ApplicationFormPage />} />
            <Route path=":id" element={<ApplicationDetailPage />} />
            <Route path=":id/edit" element={<ApplicationFormPage />} />
          </Route>
          <Route path="resumes">
            <Route index element={<ResumesListPage />} />
            <Route path="new" element={<ResumeFormPage />} />
            <Route path=":id" element={<ResumeDetailPage />} />
            <Route path=":id/edit" element={<ResumeFormPage />} />
          </Route>
          <Route path="interviews">
            <Route index element={<InterviewsListPage />} />
            <Route path="new" element={<InterviewFormPage />} />
            <Route path=":id" element={<InterviewDetailPage />} />
            <Route path=":id/edit" element={<InterviewFormPage />} />
            <Route path=":id/debrief" element={<InterviewDebriefPage />} />
          </Route>
          <Route path="prep" element={<PrepPage />} />
          <Route
            path="analytics"
            element={
              <ComingSoonPage
                icon={BarChart3}
                title="Analytics is coming soon"
                description="A funnel and conversion rates that always show their own sample size."
              />
            }
          />
          <Route
            path="recommendations"
            element={
              <ComingSoonPage
                icon={Sparkles}
                title="Recommendations are coming soon"
                description="Ranked suggestions for what to work on next, with the evidence behind every one."
              />
            }
          />
          <Route
            path="privacy"
            element={
              <ComingSoonPage
                icon={ShieldCheck}
                title="Privacy & Federated Learning controls are coming soon"
                description="See exactly what your device has ever sent — a privacy log, not just a policy."
              />
            }
          />
          <Route
            path="profile"
            element={
              <ComingSoonPage
                icon={UserRound}
                title="Profile settings are coming soon"
                description="Update your details, change your password, or export your data."
              />
            }
          />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
