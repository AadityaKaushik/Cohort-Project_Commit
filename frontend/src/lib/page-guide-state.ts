import { GUIDE_IDS, type GuideId } from '@/lib/page-guide-content'

function key(userId: string, guide: GuideId) {
  return `cohort:guide-seen:${userId}:${guide}`
}

export function isGuideSeen(userId: string, guide: GuideId): boolean {
  try {
    return window.localStorage.getItem(key(userId, guide)) === '1'
  } catch {
    return true
  }
}

export function markGuideSeen(userId: string, guide: GuideId) {
  try {
    window.localStorage.setItem(key(userId, guide), '1')
  } catch {
    // the guide is offered again on the next visit
  }
}

export function resetAllGuides(userId: string) {
  for (const guide of GUIDE_IDS) {
    try {
      window.localStorage.removeItem(key(userId, guide))
    } catch {
      // nothing stored to clear
    }
  }
}
