export type ReadinessLevel = 0 | 1 | 2 | 3

export const READINESS_LABELS: Record<ReadinessLevel, string> = { 0: 'No data yet', 1: 'Needs work', 2: 'Developing', 3: 'Strong' }

export function readinessLevel(masteryScore: number, sufficientEvidence: boolean): ReadinessLevel {
  return sufficientEvidence ? band(masteryScore).level : 0
}

export function band(score: number): { label: string; level: 1 | 2 | 3 } {
  if (score >= 0.7) {
    return { label: 'Strong', level: 3 }
  }
  if (score >= 0.4) {
    return { label: 'Developing', level: 2 }
  }
  return { label: 'Needs work', level: 1 }
}
