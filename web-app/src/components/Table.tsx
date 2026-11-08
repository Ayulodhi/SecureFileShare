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
    <div
      style={{
        overflowX: 'auto',
        width: '100%',
        border: '1px solid var(--hairline)',
        borderRadius: 'var(--radius-md)',
        background: 'rgba(11, 15, 25, 0.5)',
      }}
    >
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          textAlign: 'left',
          fontSize: '0.875rem',
        }}
      >
        <thead>
          <tr
            style={{
              background: 'rgba(255, 255, 255, 0.02)',
              borderBottom: '1px solid var(--hairline)',
            }}
          >
            {columns.map((col, idx) => (
              <th
                key={idx}
                style={{
                  padding: '0.85rem 1.15rem',
                  fontWeight: 600,
                  fontSize: '0.725rem',
                  letterSpacing: '0.05em',
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
                style={{
                  textAlign: 'center',
                  padding: '3.5rem 1rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.875rem',
                }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      border: '2px solid rgba(99, 102, 241, 0.2)',
                      borderTopColor: 'var(--accent-primary)',
                      borderRadius: '50%',
                      animation: 'spin 0.6s linear infinite',
                    }}
                  />
                  <span>Loading records...</span>
                </div>
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                style={{
                  textAlign: 'center',
                  padding: '3.5rem 1rem',
                  color: 'var(--text-muted)',
                  fontSize: '0.875rem',
                }}
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item, rowIdx) => (
              <tr
                key={rowIdx}
                style={{
                  borderBottom: rowIdx === data.length - 1 ? 'none' : '1px solid var(--hairline)',
                  transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.035)')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                {columns.map((col, colIdx) => (
                  <td
                    key={colIdx}
                    style={{
                      padding: '1rem 1.15rem',
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
