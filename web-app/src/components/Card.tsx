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
            alignItems: 'flex-start',
            marginBottom: '1.5rem',
            paddingBottom: '1rem',
            borderBottom: '1px solid var(--hairline)',
            gap: '1rem',
          }}
        >
          <div>
            {subtitle && (
              <div
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.35rem',
                }}
              >
                {subtitle}
              </div>
            )}
            {title && (
              <h2
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 600,
                  color: 'var(--text-primary)',
                  letterSpacing: '-0.02em',
                  lineHeight: 1.25,
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
