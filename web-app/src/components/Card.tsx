import { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  title?: string;
  subtitle?: string;
  headerAction?: ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

export function Card({
  children,
  title,
  subtitle,
  headerAction,
  className = '',
  style,
}: CardProps) {
  return (
    <div className={`card ${className}`} style={style}>
      {(title || headerAction) && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'baseline',
            marginBottom: '1.5rem',
            paddingBottom: '0.85rem',
            borderBottom: 'var(--border-hairline)',
          }}
        >
          <div>
            {subtitle && (
              <div
                style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.25rem',
                }}
              >
                {subtitle}
              </div>
            )}
            {title && (
              <h2
                style={{
                  fontSize: '1.15rem',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                  letterSpacing: '-0.02em',
                }}
              >
                {title}
              </h2>
            )}
          </div>
          {headerAction && <div>{headerAction}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
