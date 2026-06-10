import { AgGridReact } from 'ag-grid-react'
import { ColDef } from 'ag-grid-community'
import { useMemo } from 'react'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-quartz.css'

type Props = { columns: string[]; rows: unknown[][] }

export default function ResultGrid({ columns, rows }: Props) {
  const colDefs = useMemo<ColDef[]>(
    () =>
      columns.map((c) => ({
        field: c,
        sortable: true,
        filter: true,
        resizable: true,
        flex: 1,
        minWidth: 120,
      })),
    [columns],
  )
  const rowData = useMemo(
    () => rows.map((r) => Object.fromEntries(columns.map((c, i) => [c, r[i]]))),
    [columns, rows],
  )

  return (
    <div
      className="ag-theme-quartz rounded-xl overflow-hidden border border-ink-200/70"
      style={{ height: 440, width: '100%' }}
    >
      <AgGridReact
        rowData={rowData}
        columnDefs={colDefs}
        pagination
        paginationPageSize={20}
        rowHeight={40}
        headerHeight={42}
      />
    </div>
  )
}
