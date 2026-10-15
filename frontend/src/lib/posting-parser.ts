import { guessCompanyFromUrl } from '@/lib/company-guess'
import { cleanName, positiveInteger, url as urlRule } from '@/lib/validation'

export type PostingField =
  | 'companyName'
  | 'jobTitle'
  | 'location'
  | 'workMode'
  | 'jobType'
  | 'jobUrl'
  | 'jobDescription'
  | 'salaryMin'
  | 'salaryMax'
  | 'salaryCurrency'

export type PostingFields = Partial<Record<PostingField, string>>

const TITLE_WORDS =
  /\b(engineer|developer|analyst|intern|internship|manager|designer|scientist|architect|consultant|associate|specialist|administrator|lead|trainee|programmer|tester|qa|sde|officer|executive|coordinator)\b/i
const TITLE_STOP = /^(about|job description|description|responsibilities|requirements|we are|we're|apply|location|company|salary|overview)\b/i
const CURRENCY_SYMBOLS: Record<string, string> = { '₹': 'INR', $: 'USD', '€': 'EUR', '£': 'GBP' }
const UNIT_FACTOR: Record<string, number> = { k: 1_000, l: 100_000, lpa: 100_000, lac: 100_000, lacs: 100_000, lakh: 100_000, lakhs: 100_000, cr: 10_000_000 }
const AMOUNT = String.raw`(?:(₹|\$|€|£|INR|USD|EUR|GBP|Rs\.?)\s*)?(\d[\d,]*(?:\.\d+)?)\s*(k|lpa|lakhs?|lacs?|l|cr)?\b`
const RANGE = new RegExp(`${AMOUNT}\\s*(?:-|–|—|to)\\s*${AMOUNT}(?:\\s*(INR|USD|EUR|GBP)\\b)?`, 'gi')
const SINGLE = new RegExp(`${AMOUNT}`, 'gi')

function tidy(value: string): string {
  return value
    .replace(/[\p{Cc}\p{So}\p{Sk}\p{Co}\p{Cs}]/gu, ' ')
    .replace(/\s+/g, ' ')
    .replace(/^[\s\-–—:|·•*#>]+|[\s\-–—:|·•,.;]+$/g, '')
    .trim()
}

function cleanText(label: string, value: string, max: number): string {
  const text = tidy(value).slice(0, max).trim()
  return text !== '' && cleanName(label)(text) === null ? text : ''
}

function currencyFrom(token: string | undefined): string {
  if (!token) return ''
  if (CURRENCY_SYMBOLS[token]) return CURRENCY_SYMBOLS[token]
  return /^rs/i.test(token) ? 'INR' : token.toUpperCase()
}

function amount(raw: string, unit: string | undefined): number {
  const value = Number(raw.replace(/,/g, ''))
  const factor = unit ? (UNIT_FACTOR[unit.toLowerCase()] ?? 1) : 1
  return Math.round(value * factor)
}

function parseSalary(text: string): { min: string; max: string; currency: string } | null {
  for (const match of text.matchAll(RANGE)) {
    const [, curA, numA, unitA, curB, numB, unitB, curC] = match
    const unit = unitB ?? unitA
    const currency = currencyFrom(curA ?? curB ?? curC) || (unit && /^(lpa|l|lakhs?|lacs?|cr)$/i.test(unit) ? 'INR' : '')
    if (!currency) continue
    const min = amount(numA, unitA ?? unitB)
    const max = amount(numB, unit)
    if (min > 0 && min <= max && max < 1_000_000_000 && Number.isFinite(max)) {
      return { min: String(min), max: String(max), currency }
    }
  }
  for (const match of text.matchAll(SINGLE)) {
    const [, cur, num, unit] = match
    const currency = currencyFrom(cur) || (unit && /^(lpa|lakhs?|lacs?|cr)$/i.test(unit) ? 'INR' : '')
    if (!currency) continue
    const value = amount(num, unit)
    if (value > 0 && value < 1_000_000_000) return { min: String(value), max: '', currency }
  }
  return null
}

function earliest(text: string, patterns: [string, RegExp][]): string {
  let best = ''
  let bestIndex = Infinity
  for (const [value, pattern] of patterns) {
    const match = pattern.exec(text)
    if (match && match.index < bestIndex) {
      best = value
      bestIndex = match.index
    }
  }
  return best
}

function findWorkMode(text: string): string {
  const positive = (pattern: string) => new RegExp(`(?<!\\b(?:not|no|non)[\\s-])\\b(?:${pattern})\\b`, 'i')
  return earliest(text, [
    ['hybrid', positive('hybrid')],
    ['remote', positive('remote|work from home|wfh')],
    ['onsite', positive('on-?site|in-office|work from office|office-based')],
  ])
}

function findJobType(title: string, text: string): string {
  if (/\bintern(ship)?\b/i.test(title)) return 'internship'
  const labelled = /(?:job|employment)\s*type\s*[:-]\s*([^\n]+)/i.exec(text)?.[1] ?? ''
  return earliest(labelled || text, [
    ['full_time', /\bfull[\s-]?time\b/i],
    ['part_time', /\bpart[\s-]?time\b/i],
    ['internship', /\binternship\b|\bintern\b/i],
    ['contract', /\bcontract(or)?\b|\bfixed[\s-]term\b/i],
  ])
}

function findTitle(lines: string[], text: string): string {
  const labelled = /^\s*(?:role|position|job title|title|designation)\s*[:-]\s*(.+)$/im.exec(text)
  if (labelled) return labelled[1]
  const hiring = /\bwe(?:'re| are)\s+(?:now\s+)?hiring\s+(?:an?\s+|for\s+(?:an?\s+)?)?([^\n.,!]+?)(?:\s+(?:to|for|in|at|who)\b|[.,!\n]|$)/i.exec(text)
  if (hiring) return hiring[1]
  const head = lines.slice(0, 6).filter((line) => line.length <= 90 && !TITLE_STOP.test(line) && !/https?:\/\//i.test(line))
  const strong = head.find((line) => TITLE_WORDS.test(line) && !/[.!?]$/.test(line))
  if (strong) return strong
  return head.find((line) => line.length >= 3 && !/[.!?]$/.test(line)) ?? ''
}

function splitTitleCompany(title: string): { title: string; company: string } {
  const match = /^(.+?)\s+(?:at|@)\s+([^|,\n]+)$/i.exec(title)
  return match ? { title: match[1], company: match[2] } : { title, company: '' }
}

function findCompany(text: string, lines: string[], titleCompany: string, jobUrl: string): string {
  const labelled = /^\s*(?:company|employer|organi[sz]ation)\s*(?:name)?\s*[:-]\s*(.+)$/im.exec(text)
  if (labelled) return labelled[1]
  const about = lines.find((line) => /^about\s+(?!the\b|us\b|this\b|you\b|your\b|our\b|role\b|job\b|team\b)/i.test(line) && line.length <= 50)
  if (about) return about.replace(/^about\s+/i, '')
  if (titleCompany) return titleCompany
  const hiring = /^\s*([A-Z][\w&.' -]{1,40}?)\s+is\s+(?:hiring|looking for|seeking)\b/m.exec(text)
  if (hiring) return hiring[1]
  const atCompany = /\bhiring\s+at\s+([A-Z][\w&.' -]{1,40})/.exec(text)
  if (atCompany) return atCompany[1]
  return guessCompanyFromUrl(jobUrl)
}

function findLocation(text: string): string {
  const match = /^\s*(?:work\s+)?location\s*[:-]\s*(.+)$/im.exec(text) ?? /\bbased in\s+([A-Z][^\n.;|]+)/.exec(text)
  return match ? match[1].replace(/\(?\b(?:remote|hybrid|on-?site)\b\)?/gi, '') : ''
}

function validUrl(raw: string): string {
  const value = raw.trim()
  return value !== '' && urlRule(value) === null ? value : ''
}

export function parsePosting(rawText: string, rawUrl = ''): PostingFields {
  try {
    const text = String(rawText ?? '').replace(/\r\n?/g, '\n')
    if (text.trim() === '') return {}
    const lines = text
      .split('\n')
      .map((line) => tidy(line))
      .filter(Boolean)
    const jobUrl = validUrl(String(rawUrl ?? ''))

    const split = splitTitleCompany(tidy(findTitle(lines, text)))
    const title = cleanText('Job title', split.title, 120)
    const company = cleanText('Company', findCompany(text, lines, split.company, jobUrl), 80)
    const salary = parseSalary(text)

    const found: PostingFields = {
      jobTitle: title,
      companyName: company,
      location: cleanText('Location', findLocation(text), 100),
      workMode: findWorkMode(text),
      jobType: findJobType(title, text),
      jobUrl,
      jobDescription: text.trim(),
    }
    if (salary && positiveInteger('Salary')(salary.min) === null && (salary.max === '' || positiveInteger('Salary')(salary.max) === null)) {
      found.salaryMin = salary.min
      found.salaryMax = salary.max
      found.salaryCurrency = salary.currency
    }
    return Object.fromEntries(Object.entries(found).filter(([, value]) => value)) as PostingFields
  } catch {
    return {}
  }
}

