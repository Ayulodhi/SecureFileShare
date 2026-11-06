import { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  icon?: ReactNode;
  loading?: boolean;
}

export function Button({
  children,
  variant = 'primary',
  icon,
  loading = false,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  const variantClass =
    variant === 'primary' ? 'btn-primary' : variant === 'danger' ? 'btn-danger' : 'btn-secondary';

  return (
    <button
      className={`btn ${variantClass} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <span
          style={{
            display: 'inline-block',
            width: '14px',
            height: '14px',
            border: '2px solid rgba(255,255,255,0.3)',
            borderTopColor: '#fff',
            borderRadius: '50%',
            animation: 'spin 0.6s linear infinite',
            marginRight: '0.4rem',
          }}
        />
      ) : (
        icon && <span style={{ display: 'inline-flex', alignItems: 'center' }}>{icon}</span>
      )}
      {children}
    </button>
  );
}
