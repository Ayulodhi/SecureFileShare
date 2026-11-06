import { ReactNode } from 'react';

export interface Column<T> {
  header: string;
  accessor: (item: T) => ReactNode;
  width?: string;
}

interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  emptyMessage?: string;
  loading?: boolean;
}

export function Table<T>({
  columns,
  data,
  emptyMessage = 'No records found.',
  loading = false,
}: TableProps<T>) {
  return (
    <div style={{ overflowX: 'auto', width: '100%', border: 'var(--border-hairline)', borderRadius: 'var(--radius-sharp)' }}>
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          textAlign: 'left',
          fontSize: '0.8125rem',
          background: 'var(--bg-surface)',
        }}
      >
        <thead>
          <tr
            style={{
              background: 'var(--bg-secondary)',
              borderBottom: 'var(--border-hairline)',
            }}
          >
            {columns.map((col, idx) => (
              <th
                key={idx}
                style={{
                  padding: '0.75rem 1rem',
                  fontWeight: 700,
                  fontSize: '0.68rem',
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  width: col.width,
                }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td
                colSpan={columns.length}
                style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}
              >
                Loading records...
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item, rowIdx) => (
              <tr
                key={rowIdx}
                style={{
                  borderBottom: rowIdx === data.length - 1 ? 'none' : 'var(--border-hairline)',
                  transition: 'background 0.1s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--bg-surface-hover)')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                {columns.map((col, colIdx) => (
                  <td
                    key={colIdx}
                    style={{
                      padding: '0.85rem 1rem',
                      verticalAlign: 'middle',
                      color: 'var(--text-primary)',
                    }}
                  >
                    {col.accessor(item)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
