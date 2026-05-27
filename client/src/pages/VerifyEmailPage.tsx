import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '@/lib/api';

export default function VerifyEmailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const initialEmail = params.get('email') || '';
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const emailLocked = !!initialEmail;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); setLoading(true);
    try {
      await api<void>('/api/auth/verify-email', {
        method: 'POST', body: JSON.stringify({ email, code })
      });
      setDone(true);
      setTimeout(() => navigate('/login'), 1500);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '인증 실패');
    } finally { setLoading(false); }
  };

  return (
    <div className="max-w-md mx-auto animate-fade-in">
      <div className="card p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold tracking-tight">이메일 인증</h1>
          <p className="text-sm text-ink-500 mt-1">받으신 메일의 6자리 코드를 입력하세요.</p>
        </div>
        {done ? (
          <div className="rounded-xl border border-emerald-100 bg-emerald-50/70 p-4 text-sm text-emerald-700">
            인증 완료! 로그인 페이지로 이동합니다…
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4">
            {emailLocked ? (
              <div className="rounded-lg border border-ink-200 bg-ink-50 px-3.5 py-2.5">
                <div className="text-xs text-ink-500">인증 이메일</div>
                <div className="text-ink-800">{email}</div>
              </div>
            ) : (
              <div>
                <label className="label">이메일</label>
                <input type="email" placeholder="이메일" value={email}
                       onChange={(e) => setEmail(e.target.value)}
                       className="input" required />
              </div>
            )}
            <div>
              <label className="label">인증코드</label>
              <input type="text" placeholder="000000"
                     value={code} onChange={(e) => setCode(e.target.value)}
                     maxLength={6} pattern="\d{6}"
                     className="input text-center text-lg tracking-[0.6em] font-mono"
                     autoFocus required />
              <p className="text-xs text-ink-500 mt-1.5">코드는 10분 후 만료됩니다.</p>
            </div>
            {error && (
              <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {error}
              </p>
            )}
            <button disabled={loading} className="btn-primary w-full py-2.5">
              {loading ? '확인 중…' : '인증하기'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
