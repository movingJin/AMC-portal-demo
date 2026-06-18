import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <div className="space-y-8 animate-fade-in">
      <section className="relative overflow-hidden card p-10 sm:p-12">
        <div className="absolute -top-24 -right-24 w-72 h-72 rounded-full bg-brand-100/60 blur-3xl" />
        <div className="absolute -bottom-24 -left-24 w-72 h-72 rounded-full bg-brand-50 blur-3xl" />
        <div className="relative">
          <span className="chip mb-4 bg-brand-50 border-brand-100 text-brand-700">
            <span className="w-1.5 h-1.5 rounded-full bg-brand-500" />
            서울아산병원 데이터플랫폼
          </span>
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight text-ink-900 leading-tight">
            데이터에 더 가깝게,
            <br className="hidden sm:block" />
            <span className="bg-brand-gradient bg-clip-text text-transparent">AMC Portal</span>
          </h1>
          <p className="mt-4 text-ink-600 max-w-xl">
            게시판으로 소통하고, 자연어로 데이터를 묻고 그리드·차트로 확인하세요.
          </p>
          <div className="mt-6 flex flex-wrap gap-2">
            <Link to="/chatbot" className="btn-primary">
              데이터 챗봇 시작
            </Link>
            <Link to="/board" className="btn-secondary">
              게시판 보기
            </Link>
          </div>
        </div>
      </section>

      <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FeatureCard
          to="/board"
          title="게시판"
          desc="공지·논의·공유 게시글과 댓글을 한 곳에서."
          icon={
            <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5">
              <path
                d="M5 6h14M5 12h14M5 18h9"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          }
        />
        <FeatureCard
          to="/chatbot"
          title="데이터 챗봇"
          desc="자연어로 묻고, 그리드와 차트로 답을 받습니다."
          icon={
            <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5">
              <path
                d="M4 18V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-5 4Z"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinejoin="round"
              />
              <circle cx="9" cy="10" r="1" fill="currentColor" />
              <circle cx="12.5" cy="10" r="1" fill="currentColor" />
              <circle cx="16" cy="10" r="1" fill="currentColor" />
            </svg>
          }
        />
      </section>
    </div>
  )
}

function FeatureCard({
  to,
  title,
  desc,
  icon,
}: {
  to: string
  title: string
  desc: string
  icon: React.ReactNode
}) {
  return (
    <Link to={to} className="card card-hover p-6 flex items-start gap-4">
      <div className="grid place-items-center w-10 h-10 rounded-xl bg-brand-50 text-brand-600 shrink-0">
        {icon}
      </div>
      <div className="min-w-0">
        <h2 className="font-semibold text-ink-900">{title}</h2>
        <p className="text-sm text-ink-500 mt-0.5">{desc}</p>
      </div>
      <span className="ml-auto text-ink-300 group-hover:text-ink-500 transition">
        <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5">
          <path
            d="M9 6l6 6-6 6"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
    </Link>
  )
}
