import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '@/lib/api'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token') || ''
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await api<void>('/api/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword }),
      })
      navigate('/login')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '재설정 실패')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="max-w-md mx-auto card p-8 text-center text-red-600">
        유효하지 않은 링크입니다.
      </div>
    )
  }

  return (
    <div className="max-w-md mx-auto animate-fade-in">
      <div className="card p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">새 비밀번호 설정</h1>
          <p className="text-sm text-ink-500 mt-1">새로운 비밀번호를 입력하세요.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">새 비밀번호</label>
            <input
              type="password"
              placeholder="8자 이상"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={8}
              className="input"
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
            {loading ? '변경 중…' : '비밀번호 변경'}
          </button>
        </form>
      </div>
    </div>
  )
}
