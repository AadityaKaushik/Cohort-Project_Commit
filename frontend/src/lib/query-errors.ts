import { ApiError } from '@/lib/api-client'

export type ErrorKind = 'offline' | 'network' | 'server' | 'other'

export function classifyError(error: unknown, online: boolean): ErrorKind {
  if (!online) {
    return 'offline'
  }
  if (error instanceof ApiError) {
    return error.status >= 500 ? 'server' : 'other'
  }
  return error instanceof TypeError ? 'network' : 'other'
}

export const ERROR_COPY: Record<Exclude<ErrorKind, 'other'>, { title: string; description: string }> = {
  offline: {
    title: "You're offline",
    description: 'Reconnect to the internet and try again. Nothing you have saved is lost.',
  },
  network: {
    title: "Can't reach the server",
    description: 'The request did not get through. Check your connection and try again.',
  },
  server: {
    title: 'The server had a problem',
    description: 'This is on our side, not yours. Try again in a moment.',
  },
}
