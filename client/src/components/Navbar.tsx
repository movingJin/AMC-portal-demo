import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '@/lib/auth'
import { api } from '@/lib/api'
import clsx from 'clsx'

export default function Navbar() {
  const { user, clear } = useAuth()
  const navigate = useNavigate()

  const logout = async () => {
    try {
      await api<void>('/api/auth/logout', { method: 'POST' })
    } catch {
      // logout 실패해도 클라이언트 상태는 정리
    }
    clear()
    navigate('/login')
  }

  const navItem = ({ isActive }: { isActive: boolean }) =>
    clsx(
      'px-3 py-1.5 rounded-md text-sm transition',
      isActive ? 'text-brand-700 bg-brand-50' : 'text-ink-600 hover:text-ink-900 hover:bg-ink-100',
    )

  return (
    <nav className="sticky top-0 z-30 border-b border-ink-200/70 bg-white/75 backdrop-blur-md supports-[backdrop-filter]:bg-white/60">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-14 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2 group">
            <span className="grid place-items-center w-7 h-7 rounded-md bg-brand-gradient text-white text-xs font-bold shadow-soft">
              A
            </span>
            <span className="font-semibold text-ink-900 tracking-tight">AMC Portal</span>
          </Link>
          <div className="hidden sm:flex items-center gap-1">
            <NavLink to="/chatbot" className={navItem}>
              데이터 챗봇
            </NavLink>
            <NavLink to="/board-master" className={navItem}>
              게시판 관리
            </NavLink>
            <NavLink to="/board/1" className={navItem}>
              테스트 게시판
            </NavLink>
            <NavLink to="/board/2" className={navItem}>
              공지사항
            </NavLink>
          </div>
        </div>
        <div className="flex items-center gap-2 text-sm">
          {user ? (
            <>
              <div className="hidden sm:flex items-center gap-2 pr-2">
                <span className="grid place-items-center w-7 h-7 rounded-full bg-ink-100 text-ink-700 text-xs font-semibold">
                  {user.displayName?.[0]?.toUpperCase() ?? '?'}
                </span>
                <span className="text-ink-700">{user.displayName}</span>
              </div>
              <button onClick={logout} className="btn-ghost text-ink-500 hover:text-red-600">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-ghost">
                로그인
              </Link>
              <Link to="/signup" className="btn-primary">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
