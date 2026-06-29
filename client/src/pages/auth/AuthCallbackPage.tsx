import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { userManager } from '@/lib/keycloak'

export default function AuthCallbackPage() {
  const navigate = useNavigate()
  const handledRef = useRef(false)

  useEffect(() => {
    // StrictMode가 effect를 두 번 실행해도 인가 코드 교환은 한 번만 일어나야 한다
    // (같은 code로 두 번 요청하면 Keycloak이 두 번째 요청을 400으로 거부해 /login ↔ 콜백 리다이렉트 루프가 생긴다).
    if (handledRef.current) return
    handledRef.current = true

    userManager
      .signinRedirectCallback()
      .then((oidcUser) => {
        const redirectPath = (oidcUser.state as { redirectPath?: string } | undefined)?.redirectPath
        navigate(redirectPath && redirectPath.startsWith('/') ? redirectPath : '/', {
          replace: true,
        })
      })
      .catch(() => navigate('/login', { replace: true }))
  }, [navigate])

  return <div className="card p-10 text-center text-ink-500">로그인 처리 중…</div>
}
