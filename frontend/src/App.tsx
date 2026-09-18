import { Route, Routes } from 'react-router-dom'
import {
  BarChart3,
  Briefcase,
  FileText,
  LayoutDashboard,
  MessagesSquare,
  PencilLine,
  ShieldCheck,
  Sparkles,
  UserRound,
} from 'lucide-react'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AppShell } from '@/components/layout/AppShell'
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
          <Route
            path="applications"
            element={
              <ComingSoonPage
                icon={Briefcase}
                title="Application tracking is coming soon"
                description="Track every application from Applied through Offer or Rejected, with a full status timeline."
              />
            }
          />
          <Route
            path="resumes"
            element={
              <ComingSoonPage
                icon={FileText}
                title="Resume management is coming soon"
                description="Keep multiple tailored resume versions and see which one is actually landing interviews."
              />
            }
          />
          <Route
            path="interviews"
            element={
              <ComingSoonPage
                icon={MessagesSquare}
                title="Interview tracking is coming soon"
                description="Capture questions, feedback, and a guided debrief for every round."
              />
            }
          />
          <Route
            path="prep"
            element={
              <ComingSoonPage
                icon={PencilLine}
                title="Interview prep is coming soon"
                description="Practice tracking and topic mastery scores that fade over time, just like real skill does."
              />
            }
          />
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
