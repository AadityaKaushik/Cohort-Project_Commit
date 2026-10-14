export const QUOTES: readonly string[] = [
  'Every application is a rehearsal for the next interview.',
  'A rejection is data. Read it, then send the next one.',
  'Small, steady practice beats the all-night cram.',
  'Prepare for the conversation, not the questions.',
  'The debrief you write today is the answer you give next week.',
  'Momentum is built one follow-up at a time.',
  'Confidence is what preparation feels like from the inside.',
  'Track the work, and the progress becomes visible.',
]

export function quoteAt(index: number): string {
  return QUOTES[((index % QUOTES.length) + QUOTES.length) % QUOTES.length]
}

/** Stable for a calendar day so a page reload does not reshuffle the line. */
export function quoteOfTheDay(now: Date = new Date()): number {
  const dayNumber = Math.floor(now.getTime() / 86_400_000)
  return dayNumber % QUOTES.length
}
