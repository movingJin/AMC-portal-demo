import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'

export type BoardFile = {
  id: number
  boardId: number
  originalName: string
  contentType: string
  fileSize: number
  createdAt: string
}

type Props = {
  files: BoardFile[]
  isOwner: boolean
  onDeleted: (id: number) => void
  showDownload?: boolean
  deferDelete?: boolean
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fileIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return '🖼'
  if (contentType === 'application/pdf') return '📄'
  return '📎'
}

async function downloadFile(file: BoardFile) {
  const token = useAuth.getState().accessToken
  const res = await fetch(`/api/board/${file.boardId}/files/${file.id}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) return
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = file.originalName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export default function BoardFileList({
  files,
  isOwner,
  onDeleted,
  showDownload = true,
  deferDelete = false,
}: Props) {
  if (files.length === 0) return null

  const handleDelete = async (file: BoardFile) => {
    if (!confirm(`"${file.originalName}" 파일을 삭제하시겠습니까?`)) return
    if (!deferDelete) {
      await api<void>(`/api/board/${file.boardId}/files/${file.id}`, { method: 'DELETE' })
    }
    onDeleted(file.id)
  }

  return (
    <div className="border border-ink-100 rounded-xl overflow-hidden">
      <div className="px-4 py-2.5 bg-ink-50 border-b border-ink-100 text-xs font-medium text-ink-500 uppercase tracking-wide">
        첨부파일 {files.length}개
      </div>
      <ul className="divide-y divide-ink-100">
        {files.map((f) => (
          <li key={f.id} className="flex items-center gap-3 px-4 py-3 hover:bg-ink-50 transition">
            <span className="text-xl shrink-0">{fileIcon(f.contentType)}</span>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-ink-800 truncate">{f.originalName}</p>
              <p className="text-xs text-ink-400">{formatBytes(f.fileSize)}</p>
            </div>
            {showDownload && (
              <button onClick={() => downloadFile(f)} className="btn-secondary text-xs shrink-0">
                다운로드
              </button>
            )}
            {isOwner && (
              <button
                onClick={() => handleDelete(f)}
                className="text-xs text-ink-400 hover:text-red-600 transition shrink-0"
              >
                삭제
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  )
}
