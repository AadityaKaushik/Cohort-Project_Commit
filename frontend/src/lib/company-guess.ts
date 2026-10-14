const BOARDS_WITH_COMPANY_IN_PATH = ['greenhouse.io', 'lever.co', 'ashbyhq.com', 'workable.com']
const GENERIC_HOSTS = ['linkedin.com', 'indeed.com', 'glassdoor.com', 'naukri.com', 'wellfound.com', 'workday.com', 'myworkdayjobs.com']
const SUBDOMAIN_NOISE = new Set(['www', 'careers', 'jobs', 'boards', 'apply', 'career'])
const SECOND_LEVEL = new Set(['co', 'com', 'org', 'net', 'ac', 'gov'])

function titleCase(slug: string): string {
  return slug
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function guessCompanyFromUrl(raw: string): string {
  let url: URL
  try {
    url = new URL(raw.trim())
  } catch {
    return ''
  }
  const labels = url.hostname.toLowerCase().split('.').filter((label) => !SUBDOMAIN_NOISE.has(label))
  if (labels.length < 2) return ''

  const tldLength = labels.length >= 3 && SECOND_LEVEL.has(labels[labels.length - 2]) ? 2 : 1
  const registrable = labels.slice(-(tldLength + 1)).join('.')

  if (BOARDS_WITH_COMPANY_IN_PATH.includes(registrable)) {
    return titleCase(url.pathname.split('/').filter(Boolean)[0] ?? '')
  }
  if (GENERIC_HOSTS.includes(registrable)) return ''
  return titleCase(labels[labels.length - tldLength - 1])
}
