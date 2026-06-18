const PAGE_WINDOW = 5

function getPageNumbers(current: number, total: number): number[] {
  const half = Math.floor(PAGE_WINDOW / 2)
  let start = Math.max(0, current - half)
  const end = Math.min(total - 1, start + PAGE_WINDOW - 1)
  start = Math.max(0, end - PAGE_WINDOW + 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

type Props = {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) return null

  const pageNumbers = getPageNumbers(page, totalPages)

  return (
    <div className="flex justify-center items-center gap-1 text-sm">
      <button
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        className="w-8 h-8 flex items-center justify-center rounded border border-ink-200 text-ink-500 hover:bg-ink-50 disabled:opacity-30 disabled:cursor-not-allowed transition"
      >
        <svg viewBox="0 0 24 24" fill="none" className="w-3.5 h-3.5">
          <path d="M15 6l-6 6 6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      {pageNumbers.map((n) => (
        <button
          key={n}
          onClick={() => onChange(n)}
          className={`w-8 h-8 flex items-center justify-center rounded border tabular-nums transition ${
            n === page
              ? 'bg-brand-600 border-brand-600 text-white font-semibold'
              : 'border-ink-200 text-ink-600 hover:bg-ink-50'
          }`}
        >
          {n + 1}
        </button>
      ))}
      <button
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
        className="w-8 h-8 flex items-center justify-center rounded border border-ink-200 text-ink-500 hover:bg-ink-50 disabled:opacity-30 disabled:cursor-not-allowed transition"
      >
        <svg viewBox="0 0 24 24" fill="none" className="w-3.5 h-3.5">
          <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    </div>
  )
}
