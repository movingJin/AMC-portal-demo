import { create } from 'zustand'
import { userManager } from './keycloak'

export type UserSummary = {
  id: number
  email: string
  displayName: string
  role: string
}

type AuthState = {
  accessToken: string | null
  user: UserSummary | null
  clear: () => void
}

export const useAuth = create<AuthState>()((set) => ({
  accessToken: null,
  user: null,
  clear: () => set({ accessToken: null, user: null }),
}))

// Keycloak이 발급한 access token만으로는 로컬 user id/role을 알 수 없으므로
// (JIT provisioning은 백엔드에만 있음) 로그인/갱신 시마다 /api/auth/me로 동기화한다.
async function syncFromAccessToken(accessToken: string) {
  try {
    const res = await fetch('/api/auth/me', { headers: { Authorization: `Bearer ${accessToken}` } })
    const json = (await res.json()) as { success: boolean; data: UserSummary | null }
    if (json.success && json.data) {
      useAuth.setState({ accessToken, user: json.data })
      return
    }
  } catch {
    // 네트워크 실패 시 아래에서 상태를 비운다
  }
  useAuth.setState({ accessToken: null, user: null })
}

userManager.events.addUserLoaded((oidcUser) => {
  if (oidcUser.access_token) syncFromAccessToken(oidcUser.access_token)
})
userManager.events.addUserUnloaded(() => useAuth.getState().clear())
userManager.events.addSilentRenewError(() => useAuth.getState().clear())

userManager.getUser().then((oidcUser) => {
  if (oidcUser && !oidcUser.expired && oidcUser.access_token) {
    syncFromAccessToken(oidcUser.access_token)
  }
})
