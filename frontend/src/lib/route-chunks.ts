/** Route modules loaded on demand; calling a loader early (on hover or focus) warms the chunk so the click feels instant. */
export const loadLoginPage = () => import('@/pages/LoginPage')
export const loadRegisterPage = () => import('@/pages/RegisterPage')
export const loadDashboardPage = () => import('@/pages/dashboard/DashboardPage')

const AUTH_LOADERS: Record<string, () => Promise<unknown>> = {
  '/login': loadLoginPage,
  '/register': loadRegisterPage,
  '/': loadDashboardPage,
}

export function prefetchRoute(path: string): void {
  AUTH_LOADERS[path]?.().catch(() => undefined)
}
