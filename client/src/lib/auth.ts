import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type UserSummary = {
  id: number
  email: string
  displayName: string
  role: string
}

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
  setTokens: (a: string, r: string, u: UserSummary) => void
  clear: () => void
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setTokens: (accessToken, refreshToken, user) => set({ accessToken, refreshToken, user }),
      clear: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'amc-portal-auth' },
  ),
)
