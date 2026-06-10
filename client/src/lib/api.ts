import { useAuth } from './auth'

export type ApiResponse<T> = { success: boolean; data: T | null; message: string | null }

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

const PUBLIC_AUTH_PATHS = [
  '/login',
  '/signup',
  '/verify-email',
  '/forgot-password',
  '/reset-password',
]

function redirectToLogin() {
  const { pathname, search } = window.location
  if (PUBLIC_AUTH_PATHS.some((p) => pathname.startsWith(p))) return
  const here = pathname + search
  const target = here && here !== '/' ? `/login?redirect=${encodeURIComponent(here)}` : '/login'
  window.location.assign(target)
}

async function refreshIfPossible(): Promise<string | null> {
  const { refreshToken, setTokens } = useAuth.getState()
  if (!refreshToken) return null
  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    const json = (await res.json()) as ApiResponse<{
      accessToken: string
      refreshToken: string
      user: { id: number; email: string; displayName: string; role: string }
    }>
    if (!json.success || !json.data) return null
    setTokens(json.data.accessToken, json.data.refreshToken, json.data.user)
    return json.data.accessToken
  } catch {
    return null
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const state = useAuth.getState()
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json')
  if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`)

  let res = await fetch(path, { ...init, headers })

  if (res.status === 401 && !path.includes('/api/auth/')) {
    const refreshed = await refreshIfPossible()
    if (refreshed) {
      headers.set('Authorization', `Bearer ${refreshed}`)
      res = await fetch(path, { ...init, headers })
    } else {
      useAuth.getState().clear()
      redirectToLogin()
    }
  }

  const json = (await res.json().catch(() => ({}))) as ApiResponse<T>
  if (!res.ok || !json.success) {
    throw new ApiError(res.status, json.message || `HTTP ${res.status}`)
  }
  return json.data as T
}
