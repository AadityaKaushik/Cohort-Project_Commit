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
  { label: 'Privacy / FL', path: '/privacy', icon: ShieldCheck },
]

export const secondaryNavItems: NavItem[] = [
  { label: 'Profile', path: '/profile', icon: UserRound },
]
