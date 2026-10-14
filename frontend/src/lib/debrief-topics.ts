import type { InterviewRoundType } from '@/lib/interviews-api'

export const ROUND_TOPICS: Record<InterviewRoundType, string[]> = {
  OA: ['Arrays and strings', 'Dynamic programming', 'Time limit', 'Edge cases'],
  PHONE_SCREEN: ['Resume walkthrough', 'Motivation', 'Basic coding', 'Role fit'],
  TECHNICAL: ['Data structures', 'Algorithms', 'Complexity', 'Debugging', 'Code quality'],
  SYSTEM_DESIGN: ['Requirements', 'Data model', 'Scaling', 'Trade-offs'],
  BEHAVIOURAL: ['Conflict', 'Leadership', 'Failure', 'Teamwork'],
  HR: ['Salary', 'Notice period', 'Culture', 'Relocation'],
  FINAL: ['Team fit', 'Past projects', 'Technical depth', 'Questions for them'],
}

export const OUTCOME_CHIPS = [
  { key: 'went_well', label: 'Went well', rating: 4 },
  { key: 'okay', label: 'Okay', rating: 3 },
  { key: 'rough', label: 'Rough', rating: 2 },
] as const

const TOPICS_PREFIX = 'Came up: '

export function withTopics(text: string, topics: string[]): string {
  const rest = text
    .split('\n')
    .filter((line) => !line.startsWith(TOPICS_PREFIX))
    .join('\n')
    .trim()
  const line = topics.length > 0 ? `${TOPICS_PREFIX}${topics.join(', ')}` : ''
  return [rest, line].filter(Boolean).join('\n')
}

export function readTopics(text: string | null): string[] | null {
  const line = (text ?? '').split('\n').find((candidate) => candidate.startsWith(TOPICS_PREFIX))
  return line ? line.slice(TOPICS_PREFIX.length).split(', ') : null
}
