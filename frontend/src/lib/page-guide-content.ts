export type GuideId =
  | 'application-form'
  | 'applications'
  | 'debrief'
  | 'application-detail'
  | 'prep'
  | 'recommendations'
  | 'privacy'
  | 'directory'

export interface GuideStep {
  /** Matches a data-guide attribute on the page. Without a match the step shows as a centred card. */
  target?: string
  title: string
  body: string
}

export interface PageGuideContent {
  title: string
  steps: GuideStep[]
}

export const PAGE_GUIDES: Record<GuideId, PageGuideContent> = {
  'application-form': {
    title: 'Creating an application',
    steps: [
      { target: 'posting', title: 'Paste a posting', body: 'Paste the job text and press "Fill the form". Empty fields are filled in.' },
      { target: 'save-from-sites', title: 'Save to Cohort bookmark', body: 'Drag the "Save to Cohort" link from the Import page to your bookmarks bar. Clicking it on a job page opens this form filled in.' },
      { target: 'form', title: 'Defaults and company suggestions', body: 'Only company, role and date are required. Existing companies are suggested as you type.' },
      { target: 'form', title: 'Duplicates and warnings', body: 'Use Duplicate to start from a copy. Warnings, such as a date far in the past, can be ignored.' },
      { title: 'Importing many at once', body: 'To add many at once, use Import CSV on the Applications page.' },
    ],
  },
  applications: {
    title: 'Applications list and board',
    steps: [
      { target: 'view-switch', title: 'List and board', body: 'The page remembers which view you used last. Switch between them here.' },
      { target: 'filters', title: 'Filters and saved view', body: 'Narrow the list with the filter bar. The filters stay in the address.' },
      { target: 'list', title: 'Quick status menu', body: 'The status badge in a row opens a menu with only the moves that are allowed from that status.' },
      { target: 'board', title: 'Drag cards', body: 'Drag a card to another column, or use its move menu. A move the pipeline does not allow is refused with a message.' },
      { target: 'import', title: 'Import', body: 'Import CSV adds many applications from a spreadsheet after you check a preview.' },
    ],
  },
  debrief: {
    title: 'Writing a debrief',
    steps: [
      { target: 'outcome', title: 'Outcome', body: 'Pick the outcome. That is enough to save a debrief.' },
      { target: 'topics', title: 'Topics', body: 'Topics typical for this round type are pre-selected. Toggle the ones that actually came up.' },
      { target: 'detail', title: 'Detail and drafts', body: 'The written fields are optional. Save draft keeps what you wrote.' },
      { title: 'Prep pack', body: 'The prep pack for an interview is on the interview page, behind the "Prep pack" link.' },
    ],
  },
  'application-detail': {
    title: 'An application in detail',
    steps: [
      { target: 'status', title: 'Change status', body: 'Only the moves the pipeline allows are offered. Every change is added to the timeline.' },
      { target: 'resume-match', title: 'Resume match', body: 'Compares the skills in the job description with the attached resume. You can draft a cover letter from the result.' },
      { target: 'email-drafts', title: 'Email drafts', body: 'Drafts for you to edit and copy. Nothing is sent from Cohort.' },
      { target: 'timeline', title: 'History', body: 'The timeline lists every status change, newest first.' },
    ],
  },
  prep: {
    title: 'Interview prep',
    steps: [
      { target: 'review', title: 'Review queue', body: 'Topics and questions that are due for another look today.' },
      { target: 'tasks', title: 'Preparation tasks', body: 'Turn a weak topic into a task and tick it off when done.' },
      { target: 'mastery', title: 'Mastery and practice logging', body: 'Select a topic to see how much data its score is based on, then log practice.' },
    ],
  },
  recommendations: {
    title: 'Recommendations',
    steps: [
      { target: 'lift', title: 'Federated Lift', body: 'Off by default. When on, the ranking also uses benchmarks from people who chose to share.' },
      { target: 'cards', title: 'Accept, snooze, dismiss', body: 'Accept adds it to your plan. Snooze hides it for a while. Dismiss removes it and asks for a short reason.' },
      { target: 'cards', title: 'Evidence', body: 'Each card shows the data it rests on and how confident it is, so you can judge it yourself.' },
    ],
  },
  privacy: {
    title: 'Privacy and shared learning',
    steps: [
      { target: 'participation', title: 'Opt in or out', body: 'Sharing is off until you turn it on, and you can turn it off at any time.' },
      { target: 'schema', title: 'What is sent', body: 'The schema lists exactly which features are sent.' },
      { target: 'ledger', title: 'Privacy Ledger', body: 'Everything that was sent is recorded here. If the list is empty, nothing has been sent.' },
    ],
  },
  directory: {
    title: 'Company directory',
    steps: [
      { target: 'directory-filters', title: 'Search and role family', body: 'Find a company by name or narrow the list to one kind of role.' },
      { target: 'roles', title: 'Roles and rounds', body: 'Each company lists the rounds usually seen per role and level, and which question bank topics they test.' },
      { target: 'difficulty', title: 'Difficulty level', body: 'A general level with the basis for it. A community rating appears only once enough candidates have shared.' },
      { target: 'sources', title: 'Sources', body: 'Each pattern lists its source and when it was last checked. It may be out of date.' },
    ],
  },
}

export const GUIDE_IDS = Object.keys(PAGE_GUIDES) as GuideId[]
