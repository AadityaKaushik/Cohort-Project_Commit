export type TextProblemCode =
  | 'EMPTY'
  | 'CONTROL_CHARACTERS'
  | 'NO_LETTERS'
  | 'TOO_FEW_LETTERS'
  | 'REPEATED_CHARACTER'
  | 'REPEATED_WORD'
  | 'KEYBOARD_MASH'

export type TextHintCode = 'OFF_TOPIC' | 'CONTAINS_CONTACT_DETAILS' | 'STOPWORDS_ONLY'

const PROBLEM_MESSAGES: Record<TextProblemCode, string> = {
  EMPTY: 'Write something, or leave this blank.',
  CONTROL_CHARACTERS: 'This contains characters that cannot be saved. Remove them and try again.',
  NO_LETTERS: 'Add a few words. This has no letters in it.',
  TOO_FEW_LETTERS: 'This is mostly symbols or numbers. Add a few words.',
  REPEATED_CHARACTER: 'This repeats one character many times. Describe the round, the questions or the outcome.',
  REPEATED_WORD: 'This repeats the same word. Describe the round, the questions or the outcome.',
  KEYBOARD_MASH: 'This looks like random typing. Describe the round, the questions or the outcome.',
}

const HINT_MESSAGES: Record<TextHintCode, string> = {
  OFF_TOPIC: 'This does not look like it is about the interview or the company. You can still save it.',
  CONTAINS_CONTACT_DETAILS: 'This seems to include an email address or phone number. Check it is yours to store.',
  STOPWORDS_ONLY: 'This has only filler words. Add what happened or what was asked.',
}

const MAX_IDENTICAL_RUN = 8
const MIN_LETTER_RATIO = 0.15
const RATIO_MIN_LENGTH = 10
const REPEATED_WORD_MIN_TOKENS = 4
const OFF_TOPIC_MIN_WORDS = 5
const MASH_MIN_LENGTH = 6
const MASH_NEIGHBOUR_RATIO = 0.6

const KEYBOARD_ROWS = ['qwertyuiop', 'asdfghjkl', 'zxcvbnm']
const MASH_FRAGMENTS = ['asdf', 'qwer', 'zxcv', 'hjkl', 'uiop', 'sdfg', 'dfgh', 'fghj']
const EMAIL = /[\w.+-]+@[\w-]+\.[\w.-]+/
const PHONE = /(?<!\d)\+?\d[\d\s().-]{7,}\d(?!\d)/g
const STOPWORDS = new Set(
  (
    'a an the and or but is are was were be been it its this that these those of to in on at for with as by from i we ' +
    'you he she they me my so very just not no yes do did had has have there then than if'
  ).split(' '),
)
const TOPIC_WORDS = new Set(
  (
    'hr cv dsa ctc sql pay job role team test code easy hard hike java task case panel stage screen online system ' +
    'design growth office remote hybrid intern'
  ).split(' '),
)
const TOPIC_PREFIXES = (
  'interview question round offer reject feedback recruit salar compens benefit cultur manag technical behavio assess ' +
  'assign algorithm coding process outcome result difficult onboard hiring work compan colleag prepar topic answer ' +
  'project resume mentor stipend packag select onsite discuss balanc leetcode python react applic shortlist ghost ' +
  'explain approach solution solv stuck nervous confiden communicat mistake concept understand complexit optimi debug ' +
  'edge clear struggl time'
).split(' ')

export interface TextFinding<C extends string> {
  code: C
  message: string
}

function tokens(text: string): string[] {
  return text
    .toLowerCase()
    .split(/[^\p{L}\p{N}']+/u)
    .filter((token) => token !== '')
}

function longestIdenticalRun(text: string): number {
  let longest = 0
  let run = 0
  let previous = ''
  for (const char of text) {
    if (!/[\p{L}\p{N}]/u.test(char)) {
      run = 0
      previous = ''
      continue
    }
    run = char === previous ? run + 1 : 1
    previous = char
    longest = Math.max(longest, run)
  }
  return longest
}

function isRepeatedUnit(token: string): boolean {
  if (token.length < 6) {
    return false
  }
  for (let unit = 1; unit <= 4; unit++) {
    if (token.length % unit === 0 && token.length / unit >= 3 && token === token.slice(0, unit).repeat(token.length / unit)) {
      return true
    }
  }
  return false
}

function areNeighbours(a: string, b: string): boolean {
  for (const row of KEYBOARD_ROWS) {
    const first = row.indexOf(a)
    const second = row.indexOf(b)
    if (first >= 0 && second >= 0) {
      return Math.abs(first - second) === 1
    }
  }
  return false
}

function isMashToken(token: string): boolean {
  if (!/^[a-z]+$/.test(token)) {
    return false
  }
  if (token.length >= 4 && MASH_FRAGMENTS.some((fragment) => token.includes(fragment))) {
    return true
  }
  if (token.length < MASH_MIN_LENGTH) {
    return false
  }
  let neighbours = 0
  for (let i = 0; i < token.length - 1; i++) {
    if (areNeighbours(token[i], token[i + 1])) {
      neighbours++
    }
  }
  return neighbours / (token.length - 1) >= MASH_NEIGHBOUR_RATIO
}

function problemCode(text: string): TextProblemCode | null {
  if (text.trim() === '') {
    return 'EMPTY'
  }
  // eslint-disable-next-line no-control-regex
  if (/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]/.test(text)) {
    return 'CONTROL_CHARACTERS'
  }
  let letters = 0
  let nonSpace = 0
  for (const char of text) {
    if (!/[\s\p{Z}]/u.test(char)) {
      nonSpace++
      if (/\p{L}/u.test(char)) {
        letters++
      }
    }
  }
  if (letters === 0) {
    return 'NO_LETTERS'
  }
  if (nonSpace >= RATIO_MIN_LENGTH && letters / nonSpace < MIN_LETTER_RATIO) {
    return 'TOO_FEW_LETTERS'
  }
  if (longestIdenticalRun(text) >= MAX_IDENTICAL_RUN) {
    return 'REPEATED_CHARACTER'
  }
  const words = tokens(text)
  const allSame = words.length >= REPEATED_WORD_MIN_TOKENS && new Set(words).size === 1
  if (allSame || (words.length === 1 && isRepeatedUnit(words[0]))) {
    return 'REPEATED_WORD'
  }
  const mashed = words.filter(isMashToken).length
  if (mashed > 0 && mashed * 2 >= words.length) {
    return 'KEYBOARD_MASH'
  }
  return null
}

function containsPhoneNumber(text: string): boolean {
  return (text.match(PHONE) ?? []).some((match) => match.replace(/\D/g, '').length >= 9)
}

function isLatin(text: string): boolean {
  return [...text].filter((char) => /\p{L}/u.test(char)).every((char) => /\p{Script=Latin}/u.test(char))
}

function isTopicWord(token: string): boolean {
  return TOPIC_WORDS.has(token) || TOPIC_PREFIXES.some((prefix) => token.startsWith(prefix))
}

export function textProblem(text: string): TextFinding<TextProblemCode> | null {
  const code = problemCode(text)
  return code ? { code, message: PROBLEM_MESSAGES[code] } : null
}

export function textHints(text: string, experienceField: boolean): TextFinding<TextHintCode>[] {
  if (text.trim() === '') {
    return []
  }
  const codes: TextHintCode[] = []
  if (EMAIL.test(text) || containsPhoneNumber(text)) {
    codes.push('CONTAINS_CONTACT_DETAILS')
  }
  const words = tokens(text)
  if (words.length >= 3 && words.every((word) => STOPWORDS.has(word))) {
    codes.push('STOPWORDS_ONLY')
  } else if (experienceField && words.length >= OFF_TOPIC_MIN_WORDS && isLatin(text) && !words.some(isTopicWord)) {
    codes.push('OFF_TOPIC')
  }
  return codes.map((code) => ({ code, message: HINT_MESSAGES[code] }))
}
