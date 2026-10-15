export interface TourStep {
  path: string
  title: string
  body: string
}

export const TOUR_STEPS: TourStep[] = [
  { path: '/', title: 'Dashboard', body: 'Your action queue, next interviews and pipeline in one place. Start here each day.' },
  { path: '/applications', title: 'Applications', body: 'Log every application and move it through the stages. The board view shows the whole pipeline.' },
  { path: '/interviews', title: 'Interviews', body: 'Schedule rounds, write down the questions you were asked and add a debrief afterwards.' },
  { path: '/prep', title: 'Prep', body: 'Preparation tasks and the review queue. Finished topics feed your readiness score.' },
  { path: '/recommendations', title: 'Recommendations', body: 'Suggestions based on your own history, with the evidence behind each one.' },
  { path: '/resumes', title: 'Resumes', body: 'Keep versions per role and attach the right one to each application.' },
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
