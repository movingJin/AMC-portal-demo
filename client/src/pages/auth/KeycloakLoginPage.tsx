import { useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { login } from '@/lib/keycloak'

export default function KeycloakLoginPage() {
  const [searchParams] = useSearchParams()
  const triggeredRef = useRef(false)

  useEffect(() => {
    if (triggeredRef.current) return
    triggeredRef.current = true

    const raw = searchParams.get('redirect')
    const redirectPath = raw && raw.startsWith('/') && !raw.startsWith('//') ? raw : '/'
    login(redirectPath)
  }, [searchParams])

  return <div className="card p-10 text-center text-ink-500">로그인 페이지로 이동 중…</div>
}
