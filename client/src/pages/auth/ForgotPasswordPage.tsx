import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await api<void>('/api/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
    } catch {
      // enumeration 방지를 위해 결과 동일 처리
    }
    setDone(true)
    setLoading(false)
  }

  return (
    <div className="max-w-md mx-auto animate-fade-in">
      <div className="card p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">비밀번호 찾기</h1>
          <p className="text-sm text-ink-500 mt-1">가입한 이메일로 재설정 링크를 보내드립니다.</p>
        </div>
        {done ? (
          <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-4 text-sm text-ink-700">
            입력하신 이메일이 등록되어 있다면, 재설정 링크를 보내드렸습니다. 메일함을 확인해주세요.
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="label">이메일</label>
              <input
                type="email"
                placeholder="가입한 이메일"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input"
                required
              />
            </div>
            <button disabled={loading} className="btn-primary w-full py-2.5">
              {loading ? '전송 중…' : '재설정 링크 받기'}
            </button>
          </form>
        )}
        <div className="mt-6 text-sm text-center text-ink-500">
          <Link to="/login" className="hover:text-brand-600">
            로그인으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
