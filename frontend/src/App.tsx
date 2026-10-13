import { lazy, Suspense } from 'react'
import { Outlet, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AuthFrameSkeleton } from '@/components/layout/AuthFrame'
import { RouteProgress } from '@/components/motion/RouteProgress'
import { loadDashboardPage, loadLoginPage, loadRegisterPage } from '@/lib/route-chunks'
import { NotFoundPage } from '@/pages/NotFoundPage'

const AppShell = lazy(() => import('@/components/layout/AppShell').then((module) => ({ default: module.AppShell })))
const DashboardPage = lazy(() => loadDashboardPage().then((module) => ({ default: module.DashboardPage })))
const LoginPage = lazy(() => loadLoginPage().then((module) => ({ default: module.LoginPage })))
const RegisterPage = lazy(() => loadRegisterPage().then((module) => ({ default: module.RegisterPage })))
const ApplicationDetailPage = lazy(() => import('@/pages/applications/ApplicationDetailPage').then((module) => ({ default: module.ApplicationDetailPage })))
const ApplicationFormPage = lazy(() => import('@/pages/applications/ApplicationFormPage').then((module) => ({ default: module.ApplicationFormPage })))
const ApplicationsBoardPage = lazy(() => import('@/pages/applications/ApplicationsBoardPage').then((module) => ({ default: module.ApplicationsBoardPage })))
const ApplicationsListPage = lazy(() => import('@/pages/applications/ApplicationsListPage').then((module) => ({ default: module.ApplicationsListPage })))
const ResumeDetailPage = lazy(() => import('@/pages/resumes/ResumeDetailPage').then((module) => ({ default: module.ResumeDetailPage })))
const ResumeFormPage = lazy(() => import('@/pages/resumes/ResumeFormPage').then((module) => ({ default: module.ResumeFormPage })))
const ResumesListPage = lazy(() => import('@/pages/resumes/ResumesListPage').then((module) => ({ default: module.ResumesListPage })))
const InterviewDebriefPage = lazy(() => import('@/pages/interviews/InterviewDebriefPage').then((module) => ({ default: module.InterviewDebriefPage })))
const InterviewDetailPage = lazy(() => import('@/pages/interviews/InterviewDetailPage').then((module) => ({ default: module.InterviewDetailPage })))
const InterviewFormPage = lazy(() => import('@/pages/interviews/InterviewFormPage').then((module) => ({ default: module.InterviewFormPage })))
const InterviewsListPage = lazy(() => import('@/pages/interviews/InterviewsListPage').then((module) => ({ default: module.InterviewsListPage })))
const ContactsPage = lazy(() => import('@/pages/contacts/ContactsPage').then((module) => ({ default: module.ContactsPage })))
const CompaniesPage = lazy(() => import('@/pages/companies/CompaniesPage').then((module) => ({ default: module.CompaniesPage })))
const CompanyDetailPage = lazy(() => import('@/pages/companies/CompanyDetailPage').then((module) => ({ default: module.CompanyDetailPage })))
const PrepPage = lazy(() => import('@/pages/prep/PrepPage').then((module) => ({ default: module.PrepPage })))
const QuestionBankPage = lazy(() => import('@/pages/prep/QuestionBankPage').then((module) => ({ default: module.QuestionBankPage })))
const AnalyticsPage = lazy(() => import('@/pages/analytics/AnalyticsPage').then((module) => ({ default: module.AnalyticsPage })))
const RecommendationsPage = lazy(() => import('@/pages/recommendations/RecommendationsPage').then((module) => ({ default: module.RecommendationsPage })))
const PrivacyPage = lazy(() => import('@/pages/privacy/PrivacyPage').then((module) => ({ default: module.PrivacyPage })))
const AccountPage = lazy(() => import('@/pages/account/AccountPage').then((module) => ({ default: module.AccountPage })))
const ReportsPage = lazy(() => import('@/pages/reports/ReportsPage').then((module) => ({ default: module.ReportsPage })))
const NotificationsPage = lazy(() => import('@/pages/notifications/NotificationsPage').then((module) => ({ default: module.NotificationsPage })))
const OnboardingPage = lazy(() => import('@/pages/onboarding/OnboardingPage').then((module) => ({ default: module.OnboardingPage })))
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage').then((module) => ({ default: module.ForgotPasswordPage })))
const ResetPasswordPage = lazy(() => import('@/pages/auth/ResetPasswordPage').then((module) => ({ default: module.ResetPasswordPage })))
const OAuthCallbackPage = lazy(() => import('@/pages/auth/OAuthCallbackPage').then((module) => ({ default: module.OAuthCallbackPage })))
const VerifyEmailPage = lazy(() => import('@/pages/auth/VerifyEmailPage').then((module) => ({ default: module.VerifyEmailPage })))
const LandingPage = lazy(() => import('@/pages/landing/LandingPage').then((module) => ({ default: module.LandingPage })))

const landing = (
  <Suspense
    fallback={
      <div className="min-h-screen bg-background">
        <RouteProgress />
      </div>
    }
  >
    <LandingPage />
  </Suspense>
)

function SignedInShell() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-background">
          <RouteProgress />
        </div>
      }
    >
      <AppShell />
    </Suspense>
  )
}

function AuthRoutes() {
  return (
    <Suspense fallback={<AuthFrameSkeleton />}>
      <Outlet />
    </Suspense>
  )
}

function App() {
  return (
    <Routes>
      <Route element={<AuthRoutes />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      </Route>
      <Route element={<ProtectedRoute signedOutHome={landing} />}>
        <Route element={<AuthRoutes />}>
          <Route path="/onboarding" element={<OnboardingPage />} />
        </Route>
        <Route element={<SignedInShell />}>
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
          <Route path="contacts" element={<ContactsPage />} />
          <Route path="companies">
            <Route index element={<CompaniesPage />} />
            <Route path=":id" element={<CompanyDetailPage />} />
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
