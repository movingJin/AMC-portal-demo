import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import RichTextEditor from '@/components/RichTextEditor'

const EMPTY_HTML_RE = /^(<p><\/p>)*$/

export default function NewBoardPage() {
  const { boardMasterId, postId } = useParams<{ boardMasterId: string; postId: string }>()
  const isEdit = !!postId
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)

  useEffect(() => {
    if (!isEdit) return
    api<{ title: string; content: string }>(`/api/board/${postId}`).then((b) => {
      setTitle(b.title)
      setContent(b.content)
    })
  }, [postId])

  if (!user) {
    return <div className="card p-8 text-center text-ink-500">로그인이 필요합니다.</div>
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (EMPTY_HTML_RE.test(content.trim())) {
      setError('내용을 입력해주세요.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      if (isEdit) {
        await api<void>(`/api/board/${postId}`, {
          method: 'PUT',
          body: JSON.stringify({ title, content, boardMasterId: Number(boardMasterId) }),
        })
        navigate(`/board/${boardMasterId}/post/${postId}`)
      } else {
        const created = await api<{ id: number }>('/api/board', {
          method: 'POST',
          body: JSON.stringify({ title, content, boardMasterId: Number(boardMasterId) }),
        })
        navigate(`/board/${boardMasterId}/post/${created.id}`)
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : (isEdit ? '수정 실패' : '작성 실패'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto animate-fade-in">
      <div className="card p-8 space-y-5">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? '글 수정' : '새 글 작성'}
          </h1>
          <p className="text-sm text-ink-500 mt-1">동료들과 공유할 내용을 적어주세요.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">제목</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="제목을 입력하세요"
              className="input"
              maxLength={200}
              required
            />
          </div>
          <div>
            <label className="label">내용</label>
            <RichTextEditor value={content} onChange={setContent} />
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2 border-t border-ink-100">
            <button
              type="button"
              onClick={() =>
                navigate(isEdit ? `/board/${boardMasterId}/post/${postId}` : `/board/${boardMasterId}`)
              }
              className="btn-secondary"
            >
              취소
            </button>
            <button disabled={loading} className="btn-primary">
              {loading ? (isEdit ? '수정 중…' : '등록 중…') : (isEdit ? '수정' : '등록')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
