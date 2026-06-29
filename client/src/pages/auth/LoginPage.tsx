import KeycloakLoginPage from './KeycloakLoginPage'
import LegacyLoginPage from './LegacyLoginPage'

const isLegacy = import.meta.env.VITE_AUTH_PROVIDER === 'legacy'

export default function LoginPage() {
  return isLegacy ? <LegacyLoginPage /> : <KeycloakLoginPage />
}
