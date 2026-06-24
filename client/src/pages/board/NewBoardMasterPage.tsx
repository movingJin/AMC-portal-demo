import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'

type FormState = {
  title: string
  description: string
  type: string
  fileYn: boolean
  fileMaxCount: number
  commentYn: boolean
  useYn: boolean
}

type BoardMasterDetail = {
  id: number
  title: string
  description: string
  boardType: string
  fileYn: boolean
  fileMaxCount: number
  commentYn: boolean
  useYn: boolean
}

function Toggle({
  value,
  onChange,
  label,
}: {
  value: boolean
  onChange: (v: boolean) => void
  label: string
}) {
  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={() => onChange(!value)}
        className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none ${
          value ? 'bg-brand-600' : 'bg-ink-200'
        }`}
      >
        <span
          className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ${
            value ? 'translate-x-4' : 'translate-x-0'
          }`}
        />
      </button>
      <span className="text-sm text-ink-700">{label}</span>
    </div>
  )
}

const INITIAL_FORM: FormState = {
  title: '',
  description: '',
  type: 'GENERAL',
  fileYn: false,
  fileMaxCount: 1,
  commentYn: false,
  useYn: true,
}

export default function NewBoardMasterPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const [form, setForm] = useState<FormState>(INITIAL_FORM)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(isEdit)
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)

  useEffect(() => {
    if (!isEdit) return
    api<BoardMasterDetail>(`/api/board-master/${id}`)
      .then((data) =>
        setForm({
          title: data.title,
          description: data.description ?? '',
          type: data.boardType,
          fileYn: data.fileYn,
          fileMaxCount: data.fileMaxCount || 1,
          commentYn: data.commentYn,
          useYn: data.useYn,
        }),
      )
      .catch(() => setError('게시판 정보를 불러올 수 없습니다.'))
      .finally(() => setFetching(false))
  }, [id, isEdit])

  if (!user) {
    return <div className="card p-8 text-center text-ink-500">로그인이 필요합니다.</div>
  }

  const set =
    <K extends keyof FormState>(key: K) =>
    (val: FormState[K]) =>
      setForm((f) => ({ ...f, [key]: val }))

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (isEdit) {
        await api(`/api/board-master/${id}`, {
          method: 'PUT',
          body: JSON.stringify(form),
        })
      } else {
        await api('/api/board-master', {
          method: 'POST',
          body: JSON.stringify(form),
        })
      }
      navigate('/board-master')
    } catch (e) {
      setError(e instanceof Error ? e.message : `${isEdit ? '수정' : '등록'} 실패`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto animate-fade-in">
      <div className="card p-8 space-y-5">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? '게시판 수정' : '게시판 등록'}
          </h1>
          <p className="text-sm text-ink-500 mt-1">
            {isEdit ? '게시판 정보를 수정하세요.' : '새 게시판의 기본 정보를 입력하세요.'}
          </p>
        </div>

        {fetching ? (
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="skeleton h-10 w-full rounded-lg" />
            ))}
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="label">게시판명</label>
              <input
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="게시판명을 입력하세요"
                className="input"
                maxLength={100}
                required
              />
            </div>
            <div>
              <label className="label">설명</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="설명을 입력하세요 (선택)"
                className="textarea min-h-[120px]"
                maxLength={500}
              />
            </div>
            <div>
              <label className="label">게시판 유형</label>
              <select
                className="input"
                value={form.type}
                disabled={isEdit}
                onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
              >
                <option value="GENERAL">통합게시판</option>
                <option value="BLOG" disabled>
                  블로그형게시판 (추후 지원)
                </option>
                <option value="GUESTBOOK" disabled>
                  방명록 (추후 지원)
                </option>
              </select>
            </div>
            <div className="space-y-3">
              <div className="space-y-2">
                <Toggle
                  value={form.fileYn}
                  onChange={(v) =>
                    setForm((f) => ({
                      ...f,
                      fileYn: v,
                      fileMaxCount: v ? f.fileMaxCount || 1 : f.fileMaxCount,
                    }))
                  }
                  label="첨부파일 사용"
                />
                {form.fileYn && (
                  <div className="flex items-center gap-2 pl-12">
                    <label className="text-sm text-ink-600 shrink-0">최대 파일 개수</label>
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={form.fileMaxCount}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          fileMaxCount: Math.max(1, Number(e.target.value)),
                        }))
                      }
                      className="input w-24 text-center"
                    />
                  </div>
                )}
              </div>
              <Toggle value={form.commentYn} onChange={set('commentYn')} label="댓글 사용" />
              <Toggle value={form.useYn} onChange={set('useYn')} label="사용여부" />
            </div>
            {error && (
              <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2 pt-2 border-t border-ink-100">
              <button
                type="button"
                onClick={() => navigate('/board-master')}
                className="btn-secondary"
              >
                취소
              </button>
              <button type="submit" disabled={loading} className="btn-primary">
                {loading ? `${isEdit ? '수정' : '등록'} 중…` : isEdit ? '수정' : '등록'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
