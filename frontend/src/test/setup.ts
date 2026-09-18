import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'

// Vitest doesn't auto-detect RTL's cleanup hook unless `test.globals` is on
// (we deliberately keep explicit imports instead of globals), so it's wired
// up here: otherwise each test file's DOM nodes pile up across tests.
afterEach(() => {
  cleanup()
})
