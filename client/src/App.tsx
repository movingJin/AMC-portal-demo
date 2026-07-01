import { Routes, Route } from 'react-router-dom'
import Navbar from '@/components/Navbar'
import HomePage from '@/pages/common/HomePage'
import LoginPage from '@/pages/auth/LoginPage'
import AuthCallbackPage from '@/pages/auth/AuthCallbackPage'
import SignupPage from '@/pages/auth/SignupPage'
import VerifyEmailPage from '@/pages/auth/VerifyEmailPage'
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage'
import ResetPasswordPage from '@/pages/auth/ResetPasswordPage'
import BoardListPage from '@/pages/board/BoardListPage'
import BoardDetailPage from '@/pages/board/BoardDetailPage'
import NewBoardPage from '@/pages/board/NewBoardPage'
import ChatbotPage from '@/pages/chatbot/ChatbotPage'
import BoardMasterListPage from '@/pages/board/BoardMasterListPage'
import NewBoardMasterPage from '@/pages/board/NewBoardMasterPage'
import ProjectListPage from '@/pages/project/ProjectListPage'

const isLegacyAuth = import.meta.env.VITE_AUTH_PROVIDER === 'legacy'

export default function App() {
  return (
    <div className="min-h-screen bg-[var(--bg)] text-ink-900">
      <div className="pointer-events-none fixed inset-x-0 top-0 h-[480px] bg-hero-radial -z-0" />
      <Navbar />
      <main className="relative max-w-6xl mx-auto px-4 sm:px-6 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          {isLegacyAuth ? (
            <>
              <Route path="/signup" element={<SignupPage />} />
              <Route path="/verify-email" element={<VerifyEmailPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
            </>
          ) : (
            <Route path="/auth/callback" element={<AuthCallbackPage />} />
          )}
          <Route path="/board/:boardMasterId" element={<BoardListPage />} />
          <Route path="/board/:boardMasterId/new" element={<NewBoardPage />} />
          <Route path="/board/:boardMasterId/post/:postId" element={<BoardDetailPage />} />
          <Route path="/board/:boardMasterId/post/:postId/edit" element={<NewBoardPage />} />
          <Route path="/chatbot" element={<ChatbotPage />} />
          <Route path="/board-master" element={<BoardMasterListPage />} />
          <Route path="/board-master/new" element={<NewBoardMasterPage />} />
          <Route path="/board-master/:id/edit" element={<NewBoardMasterPage />} />
          <Route path="/projects" element={<ProjectListPage />} />
          <Route
            path="*"
            element={
              <div className="card p-10 text-center text-ink-500">페이지를 찾을 수 없습니다.</div>
            }
          />
        </Routes>
      </main>
    </div>
  )
}
