import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';

export default function NewBoardPage() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const user = useAuth((s) => s.user);

  if (!user) {
    return (
      <div className="card p-8 text-center text-ink-500">
        로그인이 필요합니다.
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); setLoading(true);
    try {
      const created = await api<{ id: number }>('/api/board', {
        method: 'POST', body: JSON.stringify({ title, content })
      });
      navigate(`/board/${created.id}`);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '작성 실패');
    } finally { setLoading(false); }
  };

  return (
    <div className="max-w-3xl mx-auto animate-fade-in">
      <div className="card p-8 space-y-5">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">새 글 작성</h1>
          <p className="text-sm text-ink-500 mt-1">동료들과 공유할 내용을 적어주세요.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">제목</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)}
                   placeholder="제목을 입력하세요"
                   className="input" maxLength={200} required />
          </div>
          <div>
            <label className="label">내용</label>
            <textarea value={content} onChange={(e) => setContent(e.target.value)}
                      placeholder="내용을 입력하세요"
                      className="textarea min-h-[320px]" required />
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2 border-t border-ink-100">
            <button type="button" onClick={() => navigate(-1)} className="btn-secondary">
              취소
            </button>
            <button disabled={loading} className="btn-primary">
              {loading ? '등록 중…' : '등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
