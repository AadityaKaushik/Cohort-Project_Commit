const SKILL_PATTERNS: Record<string, RegExp> = {
  Java: /\bjava\b(?!script)/i,
  'Spring Boot': /\bspring(\s*boot)?\b/i,
  Python: /\bpython\b/i,
  TypeScript: /\btypescript\b|\bts\b/i,
  JavaScript: /\bjavascript\b|\bjs\b/i,
  React: /\breact\b/i,
  SQL: /\bsql\b/i,
  PostgreSQL: /\bpostgres(ql)?\b/i,
  Docker: /\bdocker\b|\bcontainers?\b/i,
  Kubernetes: /\bkubernetes\b|\bk8s\b/i,
  AWS: /\baws\b|\bamazon web services\b/i,
  'REST APIs': /\brest(ful)?\b|\bapis?\b/i,
  Git: /\bgit\b/i,
  'CI/CD': /\bci\s*\/\s*cd\b|\bcontinuous (integration|delivery)\b/i,
  Testing: /\b(unit|integration)?\s*test(ing|s)?\b/i,
  'System design': /\bsystem design\b|\bdistributed systems?\b|\bscalab(le|ility)\b/i,
  Kafka: /\bkafka\b/i,
  GraphQL: /\bgraphql\b/i,
  Go: /\bgolang\b|\bgo\b(?=\s*(,|and|or|\)|\/))/i,
}

export const SAMPLE_RESUME_SKILLS: readonly string[] = [
  'Java',
  'Spring Boot',
  'TypeScript',
  'React',
  'SQL',
  'PostgreSQL',
  'Docker',
  'REST APIs',
  'Git',
  'Testing',
]

export const SAMPLE_JOB_DESCRIPTION = `Backend Engineer, Platform team

You will build REST APIs in Java with Spring Boot, backed by PostgreSQL.
We deploy with Docker and Kubernetes on AWS through a CI/CD pipeline.
Strong testing habits and an interest in system design are expected.`

export interface JobMatch {
  required: string[]
  matched: string[]
  missing: string[]
  score: number
}

export function matchJob(jobDescription: string, resumeSkills: readonly string[] = SAMPLE_RESUME_SKILLS): JobMatch {
  const required = Object.entries(SKILL_PATTERNS)
    .filter(([, pattern]) => pattern.test(jobDescription))
    .map(([skill]) => skill)
  const have = new Set(resumeSkills)
  const matched = required.filter((skill) => have.has(skill))
  const missing = required.filter((skill) => !have.has(skill))
  const score = required.length === 0 ? 0 : Math.round((matched.length / required.length) * 100)
  return { required, matched, missing, score }
}
