import { useAuth as useKeycloakAuth } from './auth.keycloak'
import { useAuth as useLegacyAuth } from './auth.legacy'

export type { UserSummary } from './auth.keycloak'

const isLegacy = import.meta.env.VITE_AUTH_PROVIDER === 'legacy'

// 두 구현 모두 accessToken/user/clear()를 공통으로 제공한다.
// legacy 전용 필드(refreshToken/setTokens)가 필요한 곳(lib/api.ts, LegacyLoginPage)은
// './auth.legacy'를 직접 import해서 쓴다.
export const useAuth = (isLegacy ? useLegacyAuth : useKeycloakAuth) as typeof useKeycloakAuth
