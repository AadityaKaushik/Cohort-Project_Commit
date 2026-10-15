import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, type Location } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { SessionExpiredScreen } from '@/components/shared/SessionExpiredScreen'

const from = { pathname: '/applications', search: '?page=2', hash: '', state: null, key: 'k' } as Location

describe('SessionExpiredScreen', () => {
  it('links to sign in and carries the page the user was on', async () => {
    function LoginProbe() {
      return <p>login page</p>
    }
    render(
      <MemoryRouter>
        <Routes>
          <Route path="/" element={<SessionExpiredScreen from={from} />} />
          <Route path="/login" element={<LoginProbe />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Your session has ended' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Sign in again' }))
    expect(screen.getByText('login page')).toBeInTheDocument()
  })
})
