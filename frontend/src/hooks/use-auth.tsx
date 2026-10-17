import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { SESSION_EXPIRED_EVENT } from '@/lib/api-client'
import { confirmEmailVerification, loginRequest, logoutRequest, registerRequest, type AuthResponse } from '@/lib/auth-api'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/lib/auth-storage'
import { getMyProfile, type UserProfile } from '@/lib/users-api'

interface AuthContextValue {
  user: UserProfile | undefined
  isLoading: boolean
  isAuthenticated: boolean
  sessionMessage: string | null
  login: (email: string, password: string) => Promise<void>
  /** Resolves to true when the account must confirm its email before it is signed in. */
  register: (email: string, password: string, displayName: string) => Promise<{ verificationRequired: boolean }>
  confirmEmail: (email: string, code: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [hasToken, setHasToken] = useState(() => Boolean(getAccessToken()))
  const [sessionMessage, setSessionMessage] = useState<string | null>(null)

  const profileQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMyProfile,
    enabled: hasToken,
    retry: false,
  })

  useEffect(() => {
    function handleSessionExpired() {
      setHasToken(false)
      setSessionMessage('Your session has expired. Please log in again.')
      queryClient.removeQueries({ queryKey: ['me'] })
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [queryClient])

  function signIn(response: AuthResponse) {
    if (!response.access_token || !response.refresh_token) {
      return false
    }
    setTokens(response.access_token, response.refresh_token)
    setSessionMessage(null)
    setHasToken(true)
    return true
  }

  async function login(email: string, password: string) {
    signIn(await loginRequest({ email, password }))
  }

  async function register(email: string, password: string, displayName: string) {
    const response = await registerRequest({ email, password, display_name: displayName })
    return { verificationRequired: !signIn(response) }
  }

  async function confirmEmail(email: string, code: string) {
    signIn(await confirmEmailVerification({ email, code }))
  }

  async function logout() {
    const refreshToken = getRefreshToken()
    clearTokens()
    setHasToken(false)
    queryClient.removeQueries({ queryKey: ['me'] })
    if (refreshToken) {
      await logoutRequest(refreshToken).catch(() => undefined)
    }
  }

  const value: AuthContextValue = {
    user: profileQuery.data,
    isLoading: hasToken && profileQuery.isLoading,
    isAuthenticated: hasToken && !profileQuery.isError,
    sessionMessage,
    login,
    register,
    confirmEmail,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
