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
        gap: '0.5rem',
        maxWidth: '380px',
        width: '100%',
      }}
    >
      {toasts.map((t) => {
        const borderColor =
          t.type === 'success'
            ? 'var(--status-clean-border)'
            : t.type === 'error'
            ? 'var(--status-danger-border)'
            : 'var(--accent-primary)';

        const Icon =
          t.type === 'success' ? CheckCircle2 : t.type === 'error' ? AlertCircle : Info;

        const iconColor =
          t.type === 'success'
            ? 'var(--status-clean)'
            : t.type === 'error'
            ? 'var(--status-danger)'
            : 'var(--accent-primary)';

        return (
          <div
            key={t.id}
            style={{
              background: 'var(--bg-surface)',
              color: 'var(--text-primary)',
              border: `1px solid ${borderColor}`,
              borderLeft: `3px solid ${borderColor}`,
              padding: '0.85rem 1rem',
              borderRadius: 'var(--radius-sharp)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '0.75rem',
              fontSize: '0.8125rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
              <Icon size={16} color={iconColor} />
              <span>{t.message}</span>
            </div>
            <button
              onClick={() => removeToast(t.id)}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                display: 'flex',
                padding: '0.2rem',
              }}
            >
              <X size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
