import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import clsx from 'clsx';

const ResultGrid = lazy(() => import('@/components/ResultGrid'));
const ResultChart = lazy(() => import('@/components/ResultChart'));

type ChatResponse = {
  answer: string;
  sql: string | null;
  columns: string[] | null;
  rows: unknown[][] | null;
  trace: { tool: string; input: unknown; output: unknown }[];
};

type Turn = { role: 'user' | 'assistant'; question?: string; resp?: ChatResponse; error?: string };

// 백엔드 deep-agent 단계 진행을 시간 기준으로 시뮬레이션.
// (백엔드가 SSE 가 아니므로 실제 단계 푸시는 불가 — 평균 소요시간 기반 가이드 표시.)
const THINKING_STAGES: { at: number; label: string; hint: string }[] = [
  { at: 0,    label: '질문 분석 중',    hint: '키워드와 값을 추출하고 있어요…' },
  { at: 1500, label: '스키마 탐색 중',  hint: '관련 테이블·컬럼을 찾고 있어요…' },
  { at: 5000, label: '값 위치 확인 중', hint: '필터 값이 어떤 컬럼에 있는지 확인 중…' },
  { at: 7500, label: 'SQL 작성 중',     hint: 'SELECT 쿼리를 구성하고 있어요…' },
  { at: 11000, label: '쿼리 실행 중',   hint: 'NL2SQL API 로 SELECT 를 실행 중…' },
  { at: 14000, label: '답변 정리 중',   hint: '결과를 사람이 읽기 쉬운 문장으로 다듬는 중…' },
];

const SUGGESTIONS = [
  '병리 결과에 Gleason 8 이상 포함된 환자번호와 처방일자를 보여줘.',
  '전립선암 환자 중 수술 받은 사람의 최근 PSA 값',
  '70대 이상 환자 비율은?',
];

function pickStage(elapsedMs: number) {
  let active = THINKING_STAGES[0];
  for (const s of THINKING_STAGES) {
    if (elapsedMs >= s.at) active = s;
  }
  return active;
}

export default function ChatbotPage() {
  const user = useAuth((s) => s.user);
  const [turns, setTurns] = useState<Turn[]>([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingStartedAt, setLoadingStartedAt] = useState<number | null>(null);
  const [elapsedMs, setElapsedMs] = useState(0);
  const [showSql, setShowSql] = useState(true);
  const [view, setView] = useState<'grid' | 'chart'>('grid');
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!loading || loadingStartedAt == null) return;
    setElapsedMs(0);
    const t = setInterval(() => setElapsedMs(Date.now() - loadingStartedAt), 250);
    return () => clearInterval(t);
  }, [loading, loadingStartedAt]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [turns, loading]);

  if (!user) {
    return (
      <div className="card p-8 text-center text-ink-500">
        로그인이 필요합니다.
      </div>
    );
  }

  const send = async (question: string) => {
    if (!question.trim() || loading) return;
    setTurns((t) => [...t, { role: 'user', question }]);
    setQ(''); setLoading(true); setLoadingStartedAt(Date.now());
    try {
      const resp = await api<ChatResponse>('/api/chatbot/ask', {
        method: 'POST', body: JSON.stringify({ question })
      });
      setTurns((t) => [...t, { role: 'assistant', resp }]);
    } catch (e: unknown) {
      setTurns((t) => [...t, { role: 'assistant', error: e instanceof Error ? e.message : '오류' }]);
    } finally { setLoading(false); setLoadingStartedAt(null); }
  };

  const ask = (e: React.FormEvent) => { e.preventDefault(); send(q.trim()); };

  const last = [...turns].reverse().find((t) => t.role === 'assistant' && t.resp);
  const result = last?.resp;
  const hasTable = !!(result?.columns && result?.rows && result.rows.length > 0);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-5 animate-fade-in">
      {/* Chat panel */}
      <section className="lg:col-span-2 card p-0 flex flex-col overflow-hidden"
               style={{ minHeight: 640 }}>
        <header className="px-5 py-4 border-b border-ink-100 flex items-center gap-2">
          <span className="grid place-items-center w-8 h-8 rounded-lg bg-brand-gradient text-white shadow-soft">
            <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
              <path d="M4 18V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-5 4Z"
                    stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            </svg>
          </span>
          <div>
            <h2 className="font-semibold text-ink-900 leading-tight">데이터 챗봇</h2>
            <p className="text-xs text-ink-500">자연어로 묻고 결과를 확인하세요</p>
          </div>
        </header>

        <div ref={scrollRef} className="flex-1 overflow-auto px-5 py-4 space-y-4">
          {turns.length === 0 && (
            <div className="space-y-3">
              <p className="text-sm text-ink-500">예시 질문으로 시작해 보세요</p>
              <div className="flex flex-col gap-2">
                {SUGGESTIONS.map((s) => (
                  <button key={s}
                          onClick={() => send(s)}
                          className="text-left text-sm rounded-xl border border-ink-200/70 bg-white hover:bg-brand-50/40 hover:border-brand-200 transition px-3.5 py-2.5 text-ink-700">
                    <span className="text-brand-500 mr-1.5">→</span>{s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {turns.map((t, i) => (
            <div key={i} className={clsx('flex', t.role === 'user' ? 'justify-end' : 'justify-start')}>
              {t.role === 'user' ? (
                <div className="max-w-[85%] bg-brand-gradient text-white rounded-2xl rounded-tr-md px-4 py-2.5 text-sm shadow-soft">
                  {t.question}
                </div>
              ) : t.error ? (
                <div className="max-w-[90%] bg-red-50 border border-red-100 text-red-700 rounded-2xl rounded-tl-md px-4 py-2.5 text-sm">
                  {t.error}
                </div>
              ) : (
                <div className="max-w-[90%] bg-ink-50 text-ink-800 rounded-2xl rounded-tl-md px-4 py-2.5 text-sm whitespace-pre-wrap leading-relaxed">
                  {t.resp?.answer}
                </div>
              )}
            </div>
          ))}

          {loading && (() => {
            const stage = pickStage(elapsedMs);
            const stageIndex = THINKING_STAGES.indexOf(stage);
            return (
              <div className="flex">
                <div className="max-w-[90%] bg-ink-50 rounded-2xl rounded-tl-md px-4 py-3 space-y-2 w-full">
                  <div className="flex items-center gap-2 text-ink-700">
                    <span className="relative inline-flex w-2 h-2">
                      <span className="absolute inset-0 rounded-full bg-brand-500 animate-ping opacity-60" />
                      <span className="relative inline-block w-2 h-2 rounded-full bg-brand-500" />
                    </span>
                    <span className="text-sm font-medium">{stage.label}</span>
                    <span className="ml-auto text-xs text-ink-400 tabular-nums">
                      {(elapsedMs / 1000).toFixed(1)}s
                    </span>
                  </div>
                  <p className="text-xs text-ink-500">{stage.hint}</p>
                  <div className="flex gap-1 pt-0.5">
                    {THINKING_STAGES.map((s, i) => (
                      <span
                        key={s.label}
                        className={clsx(
                          'h-1 flex-1 rounded-full transition-colors',
                          i <= stageIndex ? 'bg-brand-500' : 'bg-ink-200'
                        )}
                      />
                    ))}
                  </div>
                </div>
              </div>
            );
          })()}
        </div>

        <form onSubmit={ask} className="p-3 border-t border-ink-100 bg-white">
          <div className="relative">
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="자연어로 질문하세요"
              className="input pr-24"
              disabled={loading}
            />
            <button
              disabled={loading || !q.trim()}
              className="absolute right-1.5 top-1/2 -translate-y-1/2 btn-primary py-1.5 px-3"
            >
              전송
              <svg viewBox="0 0 24 24" fill="none" className="w-4 h-4">
                <path d="M5 12h14M13 6l6 6-6 6"
                      stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        </form>
      </section>

      {/* Result panel */}
      <section className="lg:col-span-3 card p-5 space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="font-semibold text-ink-900">결과</h2>
            <p className="text-xs text-ink-500">
              {hasTable
                ? `${result!.rows!.length.toLocaleString()}행 · ${result!.columns!.length}개 컬럼`
                : '아직 결과가 없습니다'}
            </p>
          </div>
          {hasTable && (
            <div className="inline-flex p-0.5 rounded-lg bg-ink-100 text-sm">
              <button onClick={() => setView('grid')}
                      className={clsx(
                        'px-3 py-1 rounded-md transition',
                        view === 'grid' ? 'bg-white shadow-soft text-ink-900' : 'text-ink-500 hover:text-ink-700'
                      )}>
                Grid
              </button>
              <button onClick={() => setView('chart')}
                      className={clsx(
                        'px-3 py-1 rounded-md transition',
                        view === 'chart' ? 'bg-white shadow-soft text-ink-900' : 'text-ink-500 hover:text-ink-700'
                      )}>
                Chart
              </button>
            </div>
          )}
        </div>

        {result?.sql && (
          <div className="rounded-xl border border-ink-200/70 overflow-hidden">
            <button onClick={() => setShowSql((v) => !v)}
                    className="w-full flex items-center justify-between px-4 py-2.5 text-xs bg-ink-50 hover:bg-ink-100 transition">
              <span className="font-mono text-ink-600">SQL</span>
              <span className="text-ink-500">{showSql ? '숨기기 ▾' : '보기 ▸'}</span>
            </button>
            {showSql && (
              <pre className="bg-ink-900 text-ink-100 p-4 overflow-auto text-xs leading-relaxed font-mono">
                {result.sql}
              </pre>
            )}
          </div>
        )}

        {hasTable ? (
          <Suspense fallback={<div className="text-sm text-ink-500">로딩…</div>}>
            {view === 'grid'
              ? <ResultGrid columns={result!.columns!} rows={result!.rows!} />
              : <ResultChart columns={result!.columns!} rows={result!.rows!} />}
          </Suspense>
        ) : (
          <div className="border border-dashed border-ink-200 rounded-xl py-16 text-center">
            <div className="mx-auto w-12 h-12 rounded-2xl bg-brand-50 text-brand-500 grid place-items-center mb-3">
              <svg viewBox="0 0 24 24" fill="none" className="w-6 h-6">
                <path d="M3 3v18h18M7 14l4-4 4 3 5-6"
                      stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
            <p className="text-sm text-ink-500">질문을 보내면 표 또는 차트가 여기에 표시됩니다.</p>
          </div>
        )}
      </section>
    </div>
  );
}
