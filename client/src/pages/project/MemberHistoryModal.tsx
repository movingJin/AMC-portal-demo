import { useEffect, useState } from 'react'
import { api } from '@/lib/api'

type HistoryItem = {
  id: number
  displayName: string
  email: string
  role: 'ADMIN' | 'CONTRIBUTOR' | 'VIEWER'
  eventType: 'JOINED' | 'REMOVED' | 'ROLE_CHANGED'
  actedByName: string
  actedAt: string
}

const ROLE_LABEL: Record<string, string> = {
  ADMIN: '관리자',
  CONTRIBUTOR: '기여자',
  VIEWER: '뷰어',
}

const EVENT_BADGE: Record<string, string> = {
  JOINED: 'bg-green-50 text-green-700',
  REMOVED: 'bg-red-50 text-red-700',
  ROLE_CHANGED: 'bg-blue-50 text-blue-700',
}

const EVENT_LABEL: Record<string, string> = {
  JOINED: '추가',
  REMOVED: '삭제',
  ROLE_CHANGED: '역할변경',
}

type Props = {
  projectId: number
  open: boolean
  onClose: () => void
}

export default function MemberHistoryModal({ projectId, open, onClose }: Props) {
  const [history, setHistory] = useState<HistoryItem[] | null>(null)

  useEffect(() => {
    if (!open) return
    setHistory(null)
    api<HistoryItem[]>(`/api/projects/${projectId}/member-history`)
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [open, projectId])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl mx-4 flex flex-col max-h-[80vh]">
        {/* 헤더 */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-ink-100">
          <div>
            <h2 className="font-semibold text-ink-900">멤버 이력</h2>
          </div>
          <button
            onClick={onClose}
            className="grid place-items-center w-7 h-7 rounded-md text-ink-400 hover:bg-ink-100 transition"
          >
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path
                d="M18 6 6 18M6 6l12 12"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </button>
        </div>

        {/* 목록 */}
        <div className="overflow-auto flex-1 px-6 py-4">
          {history === null ? (
            <p className="text-sm text-ink-400 text-center py-8">불러오는 중…</p>
          ) : history.length === 0 ? (
            <p className="text-sm text-ink-400 text-center py-8">이력이 없습니다.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-ink-500 uppercase tracking-wide border-b border-ink-100">
                  <th className="text-left py-2 pr-4">사용자</th>
                  <th className="text-left py-2 pr-4">구분</th>
                  <th className="text-left py-2 pr-4">역할</th>
                  <th className="text-left py-2 pr-4">처리자</th>
                  <th className="text-left py-2">처리일시</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-50">
                {history.map((item) => (
                  <tr key={item.id} className="hover:bg-ink-50">
                    <td className="py-2 pr-4">
                      <span className="block text-ink-800">{item.displayName}</span>
                      <span className="block text-xs text-ink-400">{item.email}</span>
                    </td>
                    <td className="py-2 pr-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${EVENT_BADGE[item.eventType]}`}
                      >
                        {EVENT_LABEL[item.eventType]}
                      </span>
                    </td>
                    <td className="py-2 pr-4 text-ink-700">{ROLE_LABEL[item.role]}</td>
                    <td className="py-2 pr-4 text-ink-700">{item.actedByName}</td>
                    <td className="py-2 text-ink-500 whitespace-nowrap">
                      {new Date(item.actedAt).toLocaleString('ko-KR')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
