import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AppShell } from '@/components/layout/AppShell'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { ApplicationDetailPage } from '@/pages/applications/ApplicationDetailPage'
import { ApplicationFormPage } from '@/pages/applications/ApplicationFormPage'
import { ApplicationsBoardPage } from '@/pages/applications/ApplicationsBoardPage'
import { ApplicationsListPage } from '@/pages/applications/ApplicationsListPage'
import { ResumeDetailPage } from '@/pages/resumes/ResumeDetailPage'
import { ResumeFormPage } from '@/pages/resumes/ResumeFormPage'
import { ResumesListPage } from '@/pages/resumes/ResumesListPage'
import { InterviewDebriefPage } from '@/pages/interviews/InterviewDebriefPage'
import { InterviewDetailPage } from '@/pages/interviews/InterviewDetailPage'
import { InterviewFormPage } from '@/pages/interviews/InterviewFormPage'
import { InterviewsListPage } from '@/pages/interviews/InterviewsListPage'
import { PrepPage } from '@/pages/prep/PrepPage'
import { QuestionBankPage } from '@/pages/prep/QuestionBankPage'
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage'
import { RecommendationsPage } from '@/pages/recommendations/RecommendationsPage'
import { PrivacyPage } from '@/pages/privacy/PrivacyPage'
import { AccountPage } from '@/pages/account/AccountPage'
import { ReportsPage } from '@/pages/reports/ReportsPage'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'
import { LoginPage } from '@/pages/LoginPage'
import { OnboardingPage } from '@/pages/onboarding/OnboardingPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { OAuthCallbackPage } from '@/pages/auth/OAuthCallbackPage'
import { VerifyEmailPage } from '@/pages/auth/VerifyEmailPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="applications">
            <Route index element={<ApplicationsListPage />} />
            <Route path="new" element={<ApplicationFormPage />} />
            <Route path="board" element={<ApplicationsBoardPage />} />
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
          <Route path="prep">
            <Route index element={<PrepPage />} />
            <Route path="questions" element={<QuestionBankPage />} />
          </Route>
          <Route path="analytics" element={<AnalyticsPage />} />
          <Route path="recommendations" element={<RecommendationsPage />} />
          <Route path="privacy" element={<PrivacyPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<AccountPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
