export interface TourStep {
  path: string
  title: string
  body: string
  /** The help page sits outside the app shell, so the tour links to it instead of moving there. */
  outsideShell?: boolean
}

export const TOUR_STEPS: TourStep[] = [
  { path: '/', title: 'Dashboard', body: 'Your action queue, upcoming interviews and pipeline in one place. A good page to start each day.' },
  { path: '/applications', title: 'Applications and board', body: 'Every role you apply to, with its status and history. Switch to the board to see the whole pipeline by stage.' },
  { path: '/interviews', title: 'Interviews and debriefs', body: 'Schedule rounds and record what you were asked. After each one, a quick debrief takes a few taps.' },
  { path: '/prep', title: 'Interview prep and review queue', body: 'Preparation tasks, topic mastery and the questions due for review today.' },
  { path: '/resumes', title: 'Resumes', body: 'Keep a version per kind of role and attach the right one to each application.' },
  { path: '/directory', title: 'Company directory', body: 'Sourced interview patterns for companies, with rounds, difficulty and where the information came from.' },
  { path: '/offers', title: 'Offers', body: 'Record offers and compare them side by side before you decide.' },
  { path: '/goals', title: 'Goals', body: 'Set weekly targets for applications, practice and follow-ups, and see how you are doing.' },
  { path: '/recommendations', title: 'Recommendations', body: 'Suggestions based on your own history, with the evidence behind each one. Accept, snooze or dismiss them.' },
  { path: '/privacy', title: 'Privacy and the Privacy Ledger', body: 'Choose whether to join the shared learning. The Privacy Ledger lists everything that was ever sent.' },
  { path: '/help', title: 'Help', body: 'Short answers, import and bookmark instructions, shortcuts, and a way to start this tour again.', outsideShell: true },
]

export const TOUR_REPLAY_EVENT = 'cohort:replay-tour'

function storageKey(userId: string) {
  return `cohort:tour-done:${userId}`
}

export function isTourDone(userId: string): boolean {
  try {
    return window.localStorage.getItem(storageKey(userId)) === '1'
  } catch {
    return false
  }
}

export function markTourDone(userId: string) {
  try {
    window.localStorage.setItem(storageKey(userId), '1')
  } catch {
    // the tour just shows again next visit
  }
}

export function clearTourDone(userId: string) {
  try {
    window.localStorage.removeItem(storageKey(userId))
  } catch {
    // nothing stored to clear
  }
}

export function replayTour() {
  window.dispatchEvent(new Event(TOUR_REPLAY_EVENT))
}
