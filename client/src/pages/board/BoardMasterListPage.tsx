import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import Pagination from '@/components/Pagination'

type BoardMaster = {
  id: number
  title: string
  authorName: string
  useYn: boolean
  createdAt: string
}

type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number }

const PAGE_SIZE = 10

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: '2-digit', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function BoardMasterListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page   = Number(searchParams.get('page') ?? '0')
  const search = searchParams.get('keyword') ?? ''

  const [data, setData] = useState<Page<BoardMaster> | null>(null)
  const [keyword, setKeyword] = useState(search)
  const [toggling, setToggling] = useState<number | null>(null)
  const user = useAuth((s) => s.user)
  const navigate = useNavigate()

  useEffect(() => {
    const url = `/api/board-master?page=${page}&size=${PAGE_SIZE}${search ? `&keyword=${encodeURIComponent(search)}` : ''}`
    api<Page<BoardMaster>>(url).then(setData)
  }, [page, search])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchParams({ ...(keyword ? { keyword } : {}), page: '0' })
  }

  const handlePageChange = (p: number) => {
    setSearchParams({ ...(search ? { keyword: search } : {}), page: String(p) })
  }

  const handleToggle = async (id: number) => {
    setToggling(id)
    try {
      const updated = await api<BoardMaster>(`/api/board-master/${id}/use-yn`, { method: 'PATCH' })
      setData((prev) =>
        prev
          ? { ...prev, content: prev.content.map((bm) => bm.id === id ? { ...bm, useYn: updated.useYn } : bm) }
          : prev,
      )
    } finally {
      setToggling(null)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <header className="flex items-end justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">게시판 관리</h1>
          <p className="text-sm text-ink-500 mt-1">
            {data ? `전체 ${data.totalElements.toLocaleString()}개의 게시판` : '불러오는 중…'}
          </p>
        </div>
        {user && (
          <Link to="/board-master/new" className="btn-primary">
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
            등록
          </Link>
        )}
      </header>

      <form onSubmit={handleSearch} className="relative">
        <svg viewBox="0 0 24 24" fill="none" className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-400">
          <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8" />
          <path d="m20 20-3.5-3.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="게시판명으로 검색"
          className="input pl-10 pr-24"
        />
        <button className="absolute right-1.5 top-1/2 -translate-y-1/2 btn-secondary py-1.5">
          검색
        </button>
      </form>

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs uppercase tracking-wider text-ink-500 bg-ink-50/60">
              <th className="px-5 py-3 w-16 font-medium">No</th>
              <th className="px-5 py-3 font-medium">게시판명</th>
              <th className="px-5 py-3 w-36 font-medium">등록자</th>
              <th className="px-5 py-3 w-44 font-medium">등록일</th>
              <th className="px-5 py-3 w-24 font-medium text-center">사용여부</th>
            </tr>
          </thead>
          <tbody>
            {!data &&
              [...Array(PAGE_SIZE)].map((_, i) => (
                <tr key={i} className="border-t border-ink-100">
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-6" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-3/5" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-20" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-28" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-5 w-10 mx-auto rounded-full" /></td>
                </tr>
              ))}
            {data?.content.map((bm, idx) => (
              <tr
                key={bm.id}
                className="border-t border-ink-100 hover:bg-ink-50/60 transition cursor-pointer"
                onClick={() => navigate(`/board-master/${bm.id}/edit`)}
              >
                <td className="px-5 py-3.5 text-ink-400 tabular-nums">
                  {page * PAGE_SIZE + idx + 1}
                </td>
                <td className="px-5 py-3.5 font-medium text-ink-800">{bm.title}</td>
                <td className="px-5 py-3.5 text-ink-600">{bm.authorName}</td>
                <td className="px-5 py-3.5 text-ink-500">{formatDate(bm.createdAt)}</td>
                <td className="px-5 py-3.5 text-center">
                  <button
                    onClick={(e) => { e.stopPropagation(); handleToggle(bm.id) }}
                    disabled={toggling === bm.id}
                    className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none disabled:opacity-50 ${
                      bm.useYn ? 'bg-brand-600' : 'bg-ink-200'
                    }`}
                    aria-label={bm.useYn ? '사용 중 (클릭하면 미사용)' : '미사용 (클릭하면 사용)'}
                  >
                    <span className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ${bm.useYn ? 'translate-x-4' : 'translate-x-0'}`} />
                  </button>
                </td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center py-16 text-ink-400">
                  <div className="text-3xl mb-2">∅</div>
                  게시판이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={page} totalPages={data?.totalPages ?? 0} onChange={handlePageChange} />
    </div>
  )
}
