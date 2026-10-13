import {
  BarChart3,
  Briefcase,
  FileText,
  FileDown,
  LayoutDashboard,
  MessagesSquare,
  PencilLine,
  ShieldCheck,
  Sparkles,
  UserRound,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  /** Live counts (open applications, upcoming interviews, ...) are wired
   * up once the relevant feature ships — a badge that never changes is
   * decoration, not information (PROJECT_PLAN.md §20.2). */
  count?: number
}

export const primaryNavItems: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: LayoutDashboard },
  { label: 'Applications', path: '/applications', icon: Briefcase },
  { label: 'Resumes', path: '/resumes', icon: FileText },
  { label: 'Interviews', path: '/interviews', icon: MessagesSquare },
  { label: 'Interview Prep', path: '/prep', icon: PencilLine },
  { label: 'Analytics', path: '/analytics', icon: BarChart3 },
  { label: 'Recommendations', path: '/recommendations', icon: Sparkles },
  { label: 'Reports', path: '/reports', icon: FileDown },
  { label: 'Privacy / FL', path: '/privacy', icon: ShieldCheck },
]

export const secondaryNavItems: NavItem[] = [
  { label: 'Profile', path: '/profile', icon: UserRound },
]

export interface Crumb {
  label: string
  path?: string
}

const SECTION_LABELS: Record<string, string> = {
  applications: 'Applications',
  resumes: 'Resumes',
  interviews: 'Interviews',
  prep: 'Interview Prep',
  analytics: 'Analytics',
  recommendations: 'Recommendations',
  reports: 'Reports',
  privacy: 'Privacy / FL',
  notifications: 'Notifications',
  profile: 'Profile',
}

const ACTION_LABELS: Record<string, string> = {
  new: 'New',
  board: 'Board',
  edit: 'Edit',
  debrief: 'Debrief',
}

export function breadcrumbsFor(pathname: string): Crumb[] {
  const segments = pathname.split('/').filter(Boolean)
  if (segments.length === 0) {
    return [{ label: 'Dashboard' }]
  }
  const sectionPath = `/${segments[0]}`
  const section: Crumb = { label: SECTION_LABELS[segments[0]] ?? 'Cohort', path: sectionPath }
  if (segments.length === 1) {
    return [{ label: section.label }]
  }
  const crumbs: Crumb[] = [section]
  let path = sectionPath
  segments.slice(1).forEach((segment, index, rest) => {
    path = `${path}/${segment}`
    const label = ACTION_LABELS[segment] ?? 'Details'
    const isLast = index === rest.length - 1
    crumbs.push(isLast ? { label } : { label, path })
  })
  return crumbs
}

export function pageTitleFor(pathname: string): string {
  const crumbs = breadcrumbsFor(pathname)
  return crumbs[crumbs.length - 1].label
}
