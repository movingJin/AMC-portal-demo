import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import BoardFileList, { type BoardFile } from '@/components/BoardFileList'

type FileHistoryItem = {
  id: number
  fileId: number
  originalName: string
  eventType: 'UPLOAD' | 'DELETE'
  actedByName: string
  actedAt: string
}

type FileDownloadItem = {
  id: number
  fileId: number
  originalName: string
  userName: string
  ipAddress: string
  downloadedAt: string
}

type HistoryModal =
  | { type: 'history'; data: FileHistoryItem[] }
  | { type: 'downloads'; data: FileDownloadItem[] }
  | null

type Board = {
  id: number
  title: string
  content: string
  authorId: number
  authorName: string
  viewCount: number
  files: BoardFile[]
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
  const { state } = useLocation()
  const listSearch: string = state?.listSearch ?? ''
  const user = useAuth((s) => s.user)
  const [board, setBoard] = useState<Board | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [comment, setComment] = useState('')
  const [historyModal, setHistoryModal] = useState<HistoryModal>(null)

  const viewedRef = useRef<string | null>(null)

  const load = async () => {
    const [b] = await Promise.all([
      api<Board>(`/api/board/${postId}`),
      api<Comment[]>(`/api/board/${postId}/comments`).then(setComments),
    ])
    setBoard(b)
  }

  useEffect(() => {
    load()
    if (viewedRef.current !== postId) {
      viewedRef.current = postId ?? null
      api<void>(`/api/board/${postId}/view`, { method: 'POST' }).catch(() => {})
    }
    /* eslint-disable-next-line */
  }, [postId])

  const openHistory = async () => {
    const data = await api<FileHistoryItem[]>(`/api/board/${postId}/files/history`)
    setHistoryModal({ type: 'history', data })
  }

  const openDownloads = async () => {
    const data = await api<FileDownloadItem[]>(`/api/board/${postId}/files/downloads`)
    setHistoryModal({ type: 'downloads', data })
  }

  const toList = () => navigate(`/board/${boardMasterId}${listSearch}`)

  const remove = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return
    await api<void>(`/api/board/${postId}`, { method: 'DELETE' })
    toList()
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

  const files = board.files ?? []
  const isOwner = user?.id === board.authorId

  const modalTitle = historyModal?.type === 'history' ? '첨부파일 이력' : '다운로드 이력'

  return (
    <>
      {historyModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setHistoryModal(null)}
        >
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-6 py-4 border-b border-ink-100">
              <h2 className="font-semibold text-ink-900">{modalTitle}</h2>
              <button
                onClick={() => setHistoryModal(null)}
                className="text-ink-400 hover:text-ink-700 text-xl leading-none"
              >
                ✕
              </button>
            </div>
            <div className="overflow-auto flex-1 px-6 py-4">
              {historyModal.type === 'history' ? (
                historyModal.data.length === 0 ? (
                  <p className="text-sm text-ink-400 text-center py-8">이력이 없습니다.</p>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="text-xs text-ink-500 uppercase tracking-wide border-b border-ink-100">
                        <th className="text-left py-2 pr-4">파일명</th>
                        <th className="text-left py-2 pr-4">구분</th>
                        <th className="text-left py-2 pr-4">처리자</th>
                        <th className="text-left py-2">처리일시</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ink-50">
                      {historyModal.data.map((h) => (
                        <tr key={h.id} className="hover:bg-ink-50">
                          <td className="py-2 pr-4 max-w-[160px]">
                            <span
                              className="block truncate text-ink-800 cursor-default"
                              title={h.originalName}
                            >
                              {h.originalName}
                            </span>
                          </td>
                          <td className="py-2 pr-4">
                            <span
                              className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${
                                h.eventType === 'UPLOAD'
                                  ? 'bg-green-50 text-green-700'
                                  : 'bg-red-50 text-red-700'
                              }`}
                            >
                              {h.eventType === 'UPLOAD' ? '업로드' : '삭제'}
                            </span>
                          </td>
                          <td className="py-2 pr-4 text-ink-700">{h.actedByName}</td>
                          <td className="py-2 text-ink-500 whitespace-nowrap">
                            {new Date(h.actedAt).toLocaleString('ko-KR')}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              ) : historyModal.data.length === 0 ? (
                <p className="text-sm text-ink-400 text-center py-8">이력이 없습니다.</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-xs text-ink-500 uppercase tracking-wide border-b border-ink-100">
                      <th className="text-left py-2 pr-4">파일명</th>
                      <th className="text-left py-2 pr-4">다운로드한 사용자</th>
                      <th className="text-left py-2 pr-4">IP</th>
                      <th className="text-left py-2">다운로드 일시</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-50">
                    {historyModal.data.map((d) => (
                      <tr key={d.id} className="hover:bg-ink-50">
                        <td className="py-2 pr-4 max-w-[160px]">
                          <span
                            className="block truncate text-ink-800 cursor-default"
                            title={d.originalName}
                          >
                            {d.originalName}
                          </span>
                        </td>
                        <td className="py-2 pr-4 text-ink-700">{d.userName}</td>
                        <td className="py-2 pr-4 text-ink-500 font-mono text-xs">{d.ipAddress}</td>
                        <td className="py-2 text-ink-500 whitespace-nowrap">
                          {new Date(d.downloadedAt).toLocaleString('ko-KR')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}
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

          <div
            className="editor-content leading-relaxed text-ink-800"
            dangerouslySetInnerHTML={{ __html: board.content }}
          />

          {files.length > 0 && (
            <div className="mt-8 pt-5 border-t border-ink-100 space-y-3">
              {isOwner && (
                <div className="flex gap-2">
                  <button onClick={openHistory} className="btn-secondary text-xs">
                    첨부파일 이력
                  </button>
                  <button onClick={openDownloads} className="btn-secondary text-xs">
                    다운로드 이력
                  </button>
                </div>
              )}
              <BoardFileList files={files} isOwner={false} onDeleted={() => {}} />
            </div>
          )}

          <div className="flex justify-between items-center mt-8 pt-5 border-t border-ink-100">
            <button onClick={toList} className="btn-secondary">
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
    </>
  )
}
