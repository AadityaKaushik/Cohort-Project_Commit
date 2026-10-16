const TOKEN_OVERRIDES: Record<string, string> = {
  dsa: 'DSA',
  hr: 'HR',
  oa: 'OA',
  os: 'OS',
  fl: 'FL',
  cv: 'CV',
  url: 'URL',
  ai: 'AI',
}

const VALUE_OVERRIDES: Record<string, string> = {
  failed: 'Not selected',
  needs_review: 'Needs review',
  system_default: 'System default',
  os_networking: 'OS and networking',
  oop: 'OOP',
  company_specific: 'Company specific',
}

export function formatLabel(value: string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const override = VALUE_OVERRIDES[value.toLowerCase()]
  if (override) {
    return override
  }
  return value
    .toLowerCase()
    .split(/[_\s-]+/)
    .map((token) => TOKEN_OVERRIDES[token] ?? token.charAt(0).toUpperCase() + token.slice(1))
    .join(' ')
}
