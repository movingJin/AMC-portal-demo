import { useMemo, useState } from 'react'
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
  Legend,
} from 'recharts'
import clsx from 'clsx'

type Props = { columns: string[]; rows: unknown[][] }

export default function ResultChart({ columns, rows }: Props) {
  const numericCols = useMemo(
    () => columns.filter((_, i) => rows.every((r) => r[i] == null || typeof r[i] === 'number')),
    [columns, rows],
  )
  const labelCol = useMemo(
    () => columns.find((c) => !numericCols.includes(c)) || columns[0],
    [columns, numericCols],
  )
  const yCols = numericCols

  const data = useMemo(
    () => rows.slice(0, 200).map((r) => Object.fromEntries(columns.map((c, i) => [c, r[i]]))),
    [columns, rows],
  )

  const [type, setType] = useState<'bar' | 'line'>('bar')

  if (yCols.length === 0) {
    return (
      <div className="border border-dashed border-ink-200 rounded-xl py-12 text-center">
        <p className="text-sm text-ink-500">차트로 표시할 수치형 컬럼이 없습니다.</p>
      </div>
    )
  }

  const palette = ['#3f60ff', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']

  return (
    <div className="space-y-3">
      <div className="inline-flex p-0.5 rounded-lg bg-ink-100 text-sm">
        <button
          onClick={() => setType('bar')}
          className={clsx(
            'px-3 py-1 rounded-md transition',
            type === 'bar'
              ? 'bg-white shadow-soft text-ink-900'
              : 'text-ink-500 hover:text-ink-700',
          )}
        >
          Bar
        </button>
        <button
          onClick={() => setType('line')}
          className={clsx(
            'px-3 py-1 rounded-md transition',
            type === 'line'
              ? 'bg-white shadow-soft text-ink-900'
              : 'text-ink-500 hover:text-ink-700',
          )}
        >
          Line
        </button>
      </div>
      <div
        className="rounded-xl border border-ink-200/70 bg-white p-3"
        style={{ width: '100%', height: 380 }}
      >
        <ResponsiveContainer>
          {type === 'bar' ? (
            <BarChart data={data} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
              <CartesianGrid stroke="#eef2f7" strokeDasharray="3 3" />
              <XAxis dataKey={labelCol} stroke="#94a3b8" tick={{ fontSize: 12 }} />
              <YAxis stroke="#94a3b8" tick={{ fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  borderRadius: 10,
                  border: '1px solid #e2e8f0',
                  boxShadow: '0 4px 16px -4px rgb(15 23 42 / 0.12)',
                  fontSize: 12,
                }}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              {yCols.map((c, i) => (
                <Bar key={c} dataKey={c} fill={palette[i % palette.length]} radius={[4, 4, 0, 0]} />
              ))}
            </BarChart>
          ) : (
            <LineChart data={data} margin={{ top: 10, right: 16, left: 0, bottom: 0 }}>
              <CartesianGrid stroke="#eef2f7" strokeDasharray="3 3" />
              <XAxis dataKey={labelCol} stroke="#94a3b8" tick={{ fontSize: 12 }} />
              <YAxis stroke="#94a3b8" tick={{ fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  borderRadius: 10,
                  border: '1px solid #e2e8f0',
                  boxShadow: '0 4px 16px -4px rgb(15 23 42 / 0.12)',
                  fontSize: 12,
                }}
              />
              <Legend wrapperStyle={{ fontSize: 12 }} />
              {yCols.map((c, i) => (
                <Line
                  key={c}
                  type="monotone"
                  dataKey={c}
                  stroke={palette[i % palette.length]}
                  strokeWidth={2}
                  dot={{ r: 3 }}
                  activeDot={{ r: 5 }}
                />
              ))}
            </LineChart>
          )}
        </ResponsiveContainer>
      </div>
    </div>
  )
}
