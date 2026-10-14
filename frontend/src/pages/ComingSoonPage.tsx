import type { LucideIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/EmptyState'

interface ComingSoonPageProps {
  icon: LucideIcon
  title: string
  description: string
}

export function ComingSoonPage({ icon, title, description }: ComingSoonPageProps) {
  return <EmptyState icon={icon} title={title} description={description} />
}
