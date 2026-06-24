import type React from 'react'

export type CommentItemProps = {
  comment: {
    id: number
    createdById: number
    createdByName: string
    content: string
    createdAt: string
  }
  currentUserId?: number
  editTarget: { id: number; content: string } | null
  onEditStart: (id: number, content: string) => void
  onEditChange: (content: string) => void
  onEditSave: (e: React.FormEvent) => void
  onEditCancel: () => void
  onDelete: (id: number) => void
  onReply?: () => void
  isReplying?: boolean
  avatarClass?: string
}

export function CommentItem({
  comment,
  currentUserId,
  editTarget,
  onEditStart,
  onEditChange,
  onEditSave,
  onEditCancel,
  onDelete,
  onReply,
  isReplying,
  avatarClass = 'w-8 h-8 bg-brand-50 text-brand-700',
}: CommentItemProps) {
  const isEditing = editTarget?.id === comment.id
  const isOwner = currentUserId === comment.createdById

  return (
    <div className="rounded-xl bg-ink-50/60 px-4 py-3 flex gap-3 items-start">
      <span
        className={`grid place-items-center rounded-full text-xs font-semibold shrink-0 ${avatarClass}`}
      >
        {comment.createdByName?.[0]?.toUpperCase() ?? '?'}
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-medium text-ink-800">{comment.createdByName}</span>
            <span className="text-xs text-ink-400">
              {new Date(comment.createdAt).toLocaleString('ko-KR')}
            </span>
          </div>
          {isOwner && !isEditing && (
            <div className="flex gap-2 shrink-0">
              <button
                onClick={() => onEditStart(comment.id, comment.content)}
                className="text-xs text-ink-400 hover:text-brand-600 transition"
              >
                수정
              </button>
              <button
                onClick={() => onDelete(comment.id)}
                className="text-xs text-ink-400 hover:text-red-600 transition"
              >
                삭제
              </button>
            </div>
          )}
        </div>
        {isEditing ? (
          <form onSubmit={onEditSave} className="mt-2 space-y-2">
            <textarea
              value={editTarget!.content}
              onChange={(e) => onEditChange(e.target.value)}
              className="textarea min-h-[72px] text-sm"
              autoFocus
              onFocus={(e) => {
                const len = e.target.value.length
                e.target.setSelectionRange(len, len)
              }}
            />
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={onEditCancel}
                className="btn-secondary text-sm py-1.5 px-3"
              >
                취소
              </button>
              <button className="btn-primary text-sm py-1.5 px-3">저장</button>
            </div>
          </form>
        ) : (
          <>
            <p className="mt-1 text-sm whitespace-pre-wrap leading-relaxed text-ink-700">
              {comment.content}
            </p>
            {onReply && (
              <div className="mt-1.5">
                <button
                  onClick={onReply}
                  className="text-xs text-ink-400 hover:text-brand-600 transition"
                >
                  {isReplying ? '취소' : '답글'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
