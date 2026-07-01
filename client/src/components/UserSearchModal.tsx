import { useEffect, useRef, useState } from 'react'
import { api } from '@/lib/api'

export type UserItem = {
  id: number
  displayName: string
  email: string
}

type Props = {
  title: string
  open: boolean
  multiSelect?: boolean
  excludeIds?: number[]
  onClose: () => void
  onConfirm: (users: UserItem[]) => void
}

export default function UserSearchModal({
  title,
  open,
  multiSelect = false,
  excludeIds = [],
  onClose,
  onConfirm,
}: Props) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<UserItem[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [selectedMap, setSelectedMap] = useState<Map<number, UserItem>>(new Map())
  const [confirming, setConfirming] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (open) {
      setQuery('')
      setSelectedIds(new Set())
      setSelectedMap(new Map())
      setConfirming(false)
      setLoading(true)
      api<UserItem[]>('/api/users/search?q=')
        .then(setResults)
        .catch(() => setResults([]))
        .finally(() => setLoading(false))
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setLoading(true)
      api<UserItem[]>(`/api/users/search?q=${encodeURIComponent(query)}`)
        .then(setResults)
        .catch(() => setResults([]))
        .finally(() => setLoading(false))
    }, 300)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [query, open])

  const toggle = (user: UserItem) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (!multiSelect) {
        next.clear()
        setSelectedMap(new Map([[user.id, user]]))
        next.add(user.id)
        return next
      }
      if (next.has(user.id)) {
        next.delete(user.id)
        setSelectedMap((m) => {
          const nm = new Map(m)
          nm.delete(user.id)
          return nm
        })
      } else {
        next.add(user.id)
        setSelectedMap((m) => new Map(m).set(user.id, user))
      }
      return next
    })
  }

  const handleConfirm = () => {
    onConfirm(Array.from(selectedMap.values()))
    onClose()
  }

  const visible = results.filter((u) => !excludeIds.includes(u.id))

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md mx-4 flex flex-col max-h-[80vh]">
        {/* 헤더 */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-ink-100">
          <h2 className="text-sm font-semibold text-ink-800">{title}</h2>
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

        {/* 검색 */}
        <div className="px-5 pt-4 pb-2 space-y-2">
          <div className="relative">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-400"
            >
              <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8" />
              <path
                d="m20 20-3.5-3.5"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
            <input
              ref={inputRef}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="이름 또는 이메일로 검색"
              className="input pl-9 w-full"
            />
          </div>
          {multiSelect && visible.length > 0 && (
            <div className="pt-2 flex justify-start">
              <button
                onClick={() => {
                  const allSelected = visible.every((u) => selectedIds.has(u.id))
                  if (allSelected) {
                    setSelectedIds((prev) => {
                      const next = new Set(prev)
                      visible.forEach((u) => {
                        next.delete(u.id)
                      })
                      return next
                    })
                    setSelectedMap((prev) => {
                      const next = new Map(prev)
                      visible.forEach((u) => next.delete(u.id))
                      return next
                    })
                  } else {
                    setSelectedIds((prev) => {
                      const next = new Set(prev)
                      visible.forEach((u) => next.add(u.id))
                      return next
                    })
                    setSelectedMap((prev) => {
                      const next = new Map(prev)
                      visible.forEach((u) => next.set(u.id, u))
                      return next
                    })
                  }
                }}
                className="text-xs text-brand-600 hover:text-brand-700 font-medium"
              >
                {visible.every((u) => selectedIds.has(u.id)) ? '전체해제' : '전체선택'}
              </button>
            </div>
          )}
        </div>

        {/* 결과 목록 */}
        <div className="flex-1 overflow-y-auto px-3 pb-3">
          {loading && (
            <div className="space-y-1 pt-1">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-3 py-2.5">
                  <div className="skeleton h-4 w-4 rounded" />
                  <div className="flex-1 space-y-1.5">
                    <div className="skeleton h-3 w-24" />
                    <div className="skeleton h-2.5 w-36" />
                  </div>
                </div>
              ))}
            </div>
          )}
          {!loading && visible.length === 0 && (
            <p className="text-center text-ink-400 text-sm py-8">검색 결과가 없습니다.</p>
          )}
          {!loading &&
            visible.map((user) => {
              const checked = selectedIds.has(user.id)
              return (
                <button
                  key={user.id}
                  onClick={() => toggle(user)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition ${checked ? 'bg-brand-50' : 'hover:bg-ink-50'}`}
                >
                  <input
                    type={multiSelect ? 'checkbox' : 'radio'}
                    readOnly
                    checked={checked}
                    className="rounded border-ink-300 text-brand-600 focus:ring-brand-500 shrink-0"
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-ink-800 truncate">{user.displayName}</p>
                    <p className="text-xs text-ink-400 truncate">{user.email}</p>
                  </div>
                </button>
              )
            })}
        </div>

        {/* 선택 현황 + 적용 버튼 */}
        <div className="flex items-center justify-between px-5 py-3 border-t border-ink-100 bg-ink-50/60">
          <span className="text-xs text-ink-500">
            {selectedIds.size > 0 ? `${selectedIds.size}명 선택됨` : '선택 없음'}
          </span>
          <div className="flex gap-2">
            <button onClick={onClose} className="btn-secondary py-1.5 text-xs">
              취소
            </button>
            <button
              onClick={() => setConfirming(true)}
              disabled={selectedIds.size === 0}
              className="btn-primary py-1.5 text-xs disabled:opacity-40"
            >
              적용
            </button>
          </div>
        </div>

        {/* 확인 모달 */}
        {confirming && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/30 rounded-xl">
            <div className="bg-white rounded-xl shadow-xl px-6 py-5 mx-6 space-y-4">
              <p className="text-sm text-ink-800 font-medium">
                {selectedIds.size}명을 적용하시겠습니까?
              </p>
              <div className="flex justify-center gap-2">
                <button
                  onClick={() => setConfirming(false)}
                  className="btn-secondary py-1.5 text-xs"
                >
                  취소
                </button>
                <button onClick={handleConfirm} className="btn-primary py-1.5 text-xs">
                  확인
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
