import { differenceInCalendarDays, format, parseISO } from 'date-fns'

export function relativeDate(iso: string, now: Date = new Date()): string {
  const days = differenceInCalendarDays(now, parseISO(iso))
  if (days === 0) return 'today'
  if (days === 1) return 'yesterday'
  if (days === -1) return 'tomorrow'
  if (days < 0) return `in ${-days} days`
  if (days < 14) return `${days} days ago`
  if (days < 60) return `${Math.floor(days / 7)} weeks ago`
  if (days < 365) return `${Math.floor(days / 30)} months ago`
  return `${Math.floor(days / 365)} ${days < 730 ? 'year' : 'years'} ago`
}

export function exactDate(iso: string): string {
  return format(parseISO(iso), 'MMM d, yyyy')
}
