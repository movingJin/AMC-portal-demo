import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';

type Board = {
  id: number;
  title: string;
  authorId: number;
  authorName: string;
  viewCount: number;
  createdAt: string;
};

type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number };

export default function BoardListPage() {
  const [data, setData] = useState<Page<Board> | null>(null);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const user = useAuth((s) => s.user);

  const load = async (kw: string, p: number) => {
    const url = `/api/board?page=${p}&size=20${kw ? `&keyword=${encodeURIComponent(kw)}` : ''}`;
    setData(await api<Page<Board>>(url));
  };

  useEffect(() => { load(keyword, page); /* eslint-disable-next-line */ }, [page]);

  return (
    <div className="space-y-6 animate-fade-in">
      <header className="flex items-end justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">게시판</h1>
          <p className="text-sm text-ink-500 mt-1">
            {data ? `전체 ${data.totalElements.toLocaleString()}개의 게시글` : '불러오는 중…'}
          </p>
        </div>
        {user && (
          <Link to="/board/new" className="btn-primary">
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
            글쓰기
          </Link>
        )}
      </header>

      <form
        onSubmit={(e) => { e.preventDefault(); setPage(0); load(keyword, 0); }}
        className="relative"
      >
        <svg viewBox="0 0 24 24" fill="none"
             className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-400">
          <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8"/>
          <path d="m20 20-3.5-3.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
        </svg>
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="제목 또는 내용으로 검색"
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
              <th className="px-5 py-3 w-16 font-medium">#</th>
              <th className="px-5 py-3 font-medium">제목</th>
              <th className="px-5 py-3 w-36 font-medium">작성자</th>
              <th className="px-5 py-3 w-20 font-medium text-right">조회</th>
              <th className="px-5 py-3 w-44 font-medium">작성일</th>
            </tr>
          </thead>
          <tbody>
            {!data && (
              [...Array(5)].map((_, i) => (
                <tr key={i} className="border-t border-ink-100">
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-6" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-3/5" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-20" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-8 ml-auto" /></td>
                  <td className="px-5 py-3.5"><div className="skeleton h-3 w-28" /></td>
                </tr>
              ))
            )}
            {data?.content.map((b) => (
              <tr key={b.id}
                  className="border-t border-ink-100 hover:bg-ink-50/60 transition">
                <td className="px-5 py-3.5 text-ink-400 tabular-nums">{b.id}</td>
                <td className="px-5 py-3.5">
                  <Link to={`/board/${b.id}`}
                        className="font-medium text-ink-800 hover:text-brand-600 transition">
                    {b.title}
                  </Link>
                </td>
                <td className="px-5 py-3.5 text-ink-600">{b.authorName}</td>
                <td className="px-5 py-3.5 text-right tabular-nums text-ink-500">
                  {b.viewCount.toLocaleString()}
                </td>
                <td className="px-5 py-3.5 text-ink-500">
                  {new Date(b.createdAt).toLocaleString('ko-KR', {
                    year: '2-digit', month: '2-digit', day: '2-digit',
                    hour: '2-digit', minute: '2-digit'
                  })}
                </td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={5} className="text-center py-16 text-ink-400">
                  <div className="text-3xl mb-2">∅</div>
                  게시글이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 text-sm">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                  className="btn-secondary">
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path d="M15 6l-6 6 6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            이전
          </button>
          <span className="px-3 py-1.5 text-ink-600 tabular-nums">
            <span className="font-semibold text-ink-900">{page + 1}</span>
            <span className="mx-1.5 text-ink-300">/</span>
            {data.totalPages}
          </span>
          <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}
                  className="btn-secondary">
            다음
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
        </div>
      )}
    </div>
  );
}
