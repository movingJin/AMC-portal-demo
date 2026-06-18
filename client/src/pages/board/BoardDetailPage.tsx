import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'

type Board = {
  id: number
  title: string
  content: string
  authorId: number
  authorName: string
  viewCount: number
  createdAt: string
  updatedAt: string
}

type Comment = {
  id: number
  boardId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
}

export default function BoardDetailPage() {
  const { boardMasterId, postId } = useParams<{ boardMasterId: string; postId: string }>()
  const navigate = useNavigate()
  const user = useAuth((s) => s.user)
  const [board, setBoard] = useState<Board | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [comment, setComment] = useState('')

  const load = async () => {
    setBoard(await api<Board>(`/api/board/${postId}`))
    setComments(await api<Comment[]>(`/api/board/${postId}/comments`))
  }

  useEffect(() => {
    load() /* eslint-disable-next-line */
  }, [postId])

  const remove = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return
    await api<void>(`/api/board/${postId}`, { method: 'DELETE' })
    navigate(`/board/${boardMasterId}`)
  }

  const addComment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!comment.trim()) return
    await api<Comment>(`/api/board/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content: comment }),
    })
    setComment('')
    setComments(await api<Comment[]>(`/api/board/${postId}/comments`))
  }

  const removeComment = async (cid: number) => {
    await api<void>(`/api/comments/${cid}`, { method: 'DELETE' })
    setComments(await api<Comment[]>(`/api/board/${postId}/comments`))
  }

  if (!board) {
    return (
      <div className="card p-8 space-y-3">
        <div className="skeleton h-8 w-1/2" />
        <div className="skeleton h-4 w-1/3" />
        <div className="skeleton h-32 w-full mt-4" />
      </div>
    )
  }
  const isOwner = user?.id === board.authorId

  return (
    <div className="space-y-5 animate-fade-in">
      <article className="card p-8 sm:p-10">
        <header className="border-b border-ink-100 pb-5 mb-6">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-ink-900">
            {board.title}
          </h1>
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
            <span className="inline-flex items-center gap-2">
              <span className="grid place-items-center w-6 h-6 rounded-full bg-ink-100 text-ink-700 text-[10px] font-semibold">
                {board.authorName?.[0]?.toUpperCase() ?? '?'}
              </span>
              <span className="text-ink-700">{board.authorName}</span>
            </span>
            <span className="text-ink-300">·</span>
            <span className="text-ink-500">
              {new Date(board.createdAt).toLocaleString('ko-KR')}
            </span>
            <span className="text-ink-300">·</span>
            <span className="chip">조회 {board.viewCount.toLocaleString()}</span>
          </div>
        </header>
        <div className="whitespace-pre-wrap leading-relaxed text-ink-800">{board.content}</div>
        <div className="flex justify-between items-center mt-8 pt-5 border-t border-ink-100">
          <button
            onClick={() => navigate(`/board/${boardMasterId}`)}
            className="btn-secondary"
          >
            목록
          </button>
          {isOwner && (
            <div className="flex gap-2">
              <button
                onClick={() => navigate(`/board/${boardMasterId}/post/${postId}/edit`)}
                className="btn-secondary"
              >
                수정
              </button>
              <button onClick={remove} className="btn-danger">
                삭제
              </button>
            </div>
          )}
        </div>
      </article>

      <section className="card p-8 space-y-5">
        <h2 className="font-semibold text-ink-900">
          댓글 <span className="text-ink-400 font-normal">({comments.length})</span>
        </h2>

        {comments.length === 0 ? (
          <p className="text-sm text-ink-400 py-4 text-center">아직 댓글이 없습니다.</p>
        ) : (
          <ul className="space-y-4">
            {comments.map((c) => (
              <li key={c.id} className="flex gap-3 items-start">
                <span className="grid place-items-center w-8 h-8 rounded-full bg-brand-50 text-brand-700 text-xs font-semibold shrink-0">
                  {c.authorName?.[0]?.toUpperCase() ?? '?'}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2">
                    <span className="text-sm font-medium text-ink-800">{c.authorName}</span>
                    <span className="text-xs text-ink-400">
                      {new Date(c.createdAt).toLocaleString('ko-KR')}
                    </span>
                  </div>
                  <p className="mt-1 text-sm whitespace-pre-wrap leading-relaxed text-ink-700">
                    {c.content}
                  </p>
                </div>
                {user?.id === c.authorId && (
                  <button
                    onClick={() => removeComment(c.id)}
                    className="text-xs text-ink-400 hover:text-red-600 transition"
                  >
                    삭제
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        {user ? (
          <form onSubmit={addComment} className="space-y-2 pt-4 border-t border-ink-100">
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="댓글을 입력하세요"
              className="textarea min-h-[88px]"
            />
            <div className="flex justify-end">
              <button className="btn-primary">등록</button>
            </div>
          </form>
        ) : (
          <p className="text-sm text-ink-500 pt-4 border-t border-ink-100">
            로그인 후 댓글을 작성할 수 있습니다.
          </p>
        )}
      </section>
    </div>
  )
}
