import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '@/lib/api'

export default function SignupPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (password !== passwordConfirm) {
      setError('비밀번호와 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    setLoading(true)
    try {
      await api<void>('/api/auth/signup', {
        method: 'POST',
        body: JSON.stringify({ email, password, passwordConfirm, displayName }),
      })
      navigate(`/verify-email?email=${encodeURIComponent(email)}`)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '회원가입 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto animate-fade-in">
      <div className="card p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">계정 만들기</h1>
          <p className="text-sm text-ink-500 mt-1">AMC Portal에 처음 오셨다면 가입해주세요.</p>
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
            <label className="label">표시 이름</label>
            <input
              type="text"
              placeholder="홍길동"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="input"
              required
            />
          </div>
          <div>
            <label className="label">비밀번호</label>
            <input
              type="password"
              placeholder="8자 이상"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
              minLength={8}
              autoComplete="new-password"
              required
            />
          </div>
          <div>
            <label className="label">비밀번호 확인</label>
            <input
              type="password"
              placeholder="다시 한 번 입력"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              className="input"
              minLength={8}
              autoComplete="new-password"
              required
            />
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <button disabled={loading} className="btn-primary w-full py-2.5">
            {loading ? '처리 중…' : '가입하기'}
          </button>
        </form>
        <p className="text-xs text-ink-500 mt-4">가입 후 이메일로 6자리 인증코드가 발송됩니다.</p>
        <div className="mt-6 text-sm text-ink-500 text-center">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-brand-600 hover:text-brand-700">
            로그인
          </Link>
        </div>
      </div>
    </div>
  )
}
