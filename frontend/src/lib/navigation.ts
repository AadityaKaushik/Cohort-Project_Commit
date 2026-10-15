import {
  BarChart3,
  Briefcase,
  Building2,
  FileText,
  FileDown,
  HandCoins,
  Target,
  LayoutDashboard,
  MessagesSquare,
  PencilLine,
  UsersRound,
  ShieldCheck,
  Sparkles,
  UserRound,
  type LucideIcon,
} from 'lucide-react'

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  // Live counts (open applications, upcoming interviews, ...) are wired up once the relevant feature ships — a badge that never changes is decoration, not information (PROJECT_PLAN.md §20.2).
  count?: number
}

export interface NavGroup {
  label: string
  collapsed?: boolean
  items: NavItem[]
}

export const navGroups: NavGroup[] = [
  {
    label: 'Track',
    items: [
      { label: 'Dashboard', path: '/', icon: LayoutDashboard },
      { label: 'Applications', path: '/applications', icon: Briefcase },
      { label: 'Contacts', path: '/contacts', icon: UsersRound },
      { label: 'Companies', path: '/companies', icon: Building2 },
      { label: 'Offers', path: '/offers', icon: HandCoins },
    ],
  },
  {
    label: 'Prepare',
    items: [
      { label: 'Interviews', path: '/interviews', icon: MessagesSquare },
      { label: 'Interview Prep', path: '/prep', icon: PencilLine },
      { label: 'Resumes', path: '/resumes', icon: FileText },
    ],
  },
  {
    label: 'Insights',
    items: [
      { label: 'Analytics', path: '/analytics', icon: BarChart3 },
      { label: 'Recommendations', path: '/recommendations', icon: Sparkles },
      { label: 'Goals', path: '/goals', icon: Target },
    ],
  },
  {
    label: 'More',
    collapsed: true,
    items: [
      { label: 'Reports', path: '/reports', icon: FileDown },
      { label: 'Privacy / FL', path: '/privacy', icon: ShieldCheck },
    ],
  },
]

export const primaryNavItems: NavItem[] = navGroups.flatMap((group) => group.items)

export const mobileNavItems: NavItem[] = ['/', '/applications', '/interviews', '/prep'].map(
  (path) => primaryNavItems.find((item) => item.path === path)!,
)

export function isNavItemActive(pathname: string, itemPath: string): boolean {
  if (itemPath === '/') {
    return pathname === '/'
  }
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}

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
  contacts: 'Contacts',
  offers: 'Offers',
  goals: 'Goals',
  companies: 'Companies',
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
  questions: 'Question bank',
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

const RECORD_NOUNS: Record<string, string> = {
  applications: 'application',
  resumes: 'resume',
  interviews: 'interview',
}

const PAGE_TITLES: Record<string, string> = {
  board: 'Application board',
  questions: 'Question bank',
  debrief: 'Interview debrief',
}

export function pageTitleFor(pathname: string): string {
  const segments = pathname.split('/').filter(Boolean)
  const crumbs = breadcrumbsFor(pathname)
  const noun = RECORD_NOUNS[segments[0]]
  const last = segments[segments.length - 1]
  if (segments.length < 2 || !last) {
    return crumbs[crumbs.length - 1].label
  }
  if (PAGE_TITLES[last]) {
    return PAGE_TITLES[last]
  }
  if (!noun) {
    return crumbs[crumbs.length - 1].label
  }
  if (last === 'new') {
    return `New ${noun}`
  }
  if (last === 'edit') {
    return `Edit ${noun}`
  }
  return `${noun.charAt(0).toUpperCase()}${noun.slice(1)} details`
}
