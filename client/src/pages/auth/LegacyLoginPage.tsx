import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.legacy'

export default function LegacyLoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const setTokens = useAuth((s) => s.setTokens)

  const redirectTarget = (() => {
    const raw = searchParams.get('redirect')
    if (!raw || !raw.startsWith('/') || raw.startsWith('//')) return '/'
    return raw
  })()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const data = await api<{
        accessToken: string
        refreshToken: string
        user: { id: number; email: string; displayName: string; role: string }
      }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      setTokens(data.accessToken, data.refreshToken, data.user)
      navigate(redirectTarget, { replace: true })
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '로그인 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto animate-fade-in">
      <div className="card p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">다시 만나서 반가워요</h1>
          <p className="text-sm text-ink-500 mt-1">AMC Portal 계정으로 로그인하세요.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">이메일</label>
            <input
              type="email"
              placeholder="you@amc.seoul.kr"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input"
              autoComplete="email"
              required
            />
          </div>
          <div>
            <label className="label">비밀번호</label>
            <input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
              autoComplete="current-password"
              required
            />
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <button disabled={loading} className="btn-primary w-full py-2.5">
            {loading ? '로그인 중…' : '로그인'}
          </button>
        </form>
        <div className="mt-6 flex justify-between text-sm text-ink-500">
          <Link to="/signup" className="hover:text-brand-600 transition">
            회원가입
          </Link>
          <Link to="/forgot-password" className="hover:text-brand-600 transition">
            비밀번호 찾기
          </Link>
        </div>
      </div>
    </div>
  )
}
