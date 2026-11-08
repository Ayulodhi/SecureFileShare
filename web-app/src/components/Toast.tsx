import { useState, useEffect } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  message: string;
}

let toastListener: ((toast: ToastMessage) => void) | null = null;

export const showToast = (message: string, type: ToastType = 'info') => {
  if (toastListener) {
    toastListener({ id: Math.random().toString(36).substring(2, 9), type, message });
  }
};

export function ToastContainer() {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    toastListener = (newToast) => {
      setToasts((prev) => [...prev, newToast]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== newToast.id));
      }, 4500);
    };

    return () => {
      toastListener = null;
    };
  }, []);

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  if (toasts.length === 0) return null;

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '2rem',
        right: '2rem',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '0.65rem',
        maxWidth: '400px',
        width: '100%',
      }}
    >
      {toasts.map((t) => {
        const isSuccess = t.type === 'success';
        const isError = t.type === 'error';

        const borderColor = isSuccess
          ? 'var(--status-clean-border)'
          : isError
          ? 'var(--status-danger-border)'
          : 'rgba(99, 102, 241, 0.4)';

        const accentColor = isSuccess
          ? 'var(--status-clean)'
          : isError
          ? 'var(--status-danger)'
          : 'var(--accent-primary)';

        const Icon = isSuccess ? CheckCircle2 : isError ? AlertCircle : Info;

        return (
          <div
            key={t.id}
            style={{
              background: 'rgba(16, 21, 35, 0.92)',
              backdropFilter: 'blur(20px)',
              WebkitBackdropFilter: 'blur(20px)',
              color: 'var(--text-primary)',
              border: `1px solid ${borderColor}`,
              borderLeft: `4px solid ${accentColor}`,
              padding: '0.9rem 1.15rem',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '0.85rem',
              fontSize: '0.875rem',
              boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.6), 0 0 15px -3px rgba(0, 0, 0, 0.3)',
              animation: 'floatSoft 3s ease-in-out infinite',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Icon size={18} color={accentColor} style={{ flexShrink: 0 }} />
              <span style={{ lineHeight: 1.4, fontWeight: 500 }}>{t.message}</span>
            </div>
            <button
              onClick={() => removeToast(t.id)}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                display: 'flex',
                padding: '0.25rem',
                borderRadius: 'var(--radius-sm)',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = '#ffffff';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'var(--text-muted)';
              }}
            >
              <X size={15} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
