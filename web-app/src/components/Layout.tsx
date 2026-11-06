import { ReactNode } from 'react';
import { useAuth } from '../state/AuthContext';
import {
  Shield,
  UploadCloud,
  Share2,
  FileText,
  Key,
  LogOut,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

export type NavTab = 'files' | 'upload' | 'shares' | 'audit' | 'keys';

interface LayoutProps {
  currentTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  children: ReactNode;
}

export function Layout({ currentTab, onSelectTab, children }: LayoutProps) {
  const { user, hasKeys, logout } = useAuth();

  const navItems = [
    { id: 'files', label: 'Files', icon: FileText, number: '01' },
    { id: 'upload', label: 'Upload & Encrypt', icon: UploadCloud, number: '02' },
    { id: 'shares', label: 'Share Links', icon: Share2, number: '03' },
    { id: 'audit', label: 'Audit Trail', icon: Shield, number: '04' },
    { id: 'keys', label: 'Key Vault', icon: Key, number: '05' },
  ] as const;

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Swiss Architectural Sidebar */}
      <aside
        style={{
          width: '270px',
          background: 'var(--bg-secondary)',
          borderRight: 'var(--border-hairline)',
          display: 'flex',
          flexDirection: 'column',
          padding: '2rem 1.5rem',
          flexShrink: 0,
        }}
      >
        {/* Brand Header */}
        <div style={{ marginBottom: '2.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.4rem' }}>
            <div
              style={{
                width: '28px',
                height: '28px',
                background: 'var(--accent-primary)',
                borderRadius: 'var(--radius-sharp)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Shield size={16} color="#ffffff" strokeWidth={2.5} />
            </div>
            <h1
              style={{
                fontSize: '1rem',
                fontWeight: 700,
                letterSpacing: '-0.02em',
                color: 'var(--text-primary)',
              }}
            >
              SecureFileShare
            </h1>
          </div>
          <p
            style={{
              fontSize: '0.68rem',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              fontWeight: 600,
            }}
          >
            Zero-Knowledge Vault
          </p>
        </div>

        {/* Section Label */}
        <div
          style={{
            fontSize: '0.68rem',
            fontWeight: 700,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
            marginBottom: '0.75rem',
            paddingLeft: '0.5rem',
          }}
        >
          Navigation
        </div>

        {/* Navigation Tabs */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', flex: 1 }}>
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onSelectTab(item.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  padding: '0.65rem 0.75rem',
                  borderRadius: 'var(--radius-sharp)',
                  background: active ? 'var(--bg-surface)' : 'transparent',
                  color: active ? 'var(--text-primary)' : 'var(--text-secondary)',
                  borderLeft: active ? '2px solid var(--accent-primary)' : '2px solid transparent',
                  borderTop: 'none',
                  borderRight: 'none',
                  borderBottom: 'none',
                  cursor: 'pointer',
                  fontSize: '0.8125rem',
                  fontWeight: active ? 600 : 500,
                  transition: 'all 0.1s ease',
                  textAlign: 'left',
                }}
                onMouseEnter={(e) => {
                  if (!active) {
                    e.currentTarget.style.background = 'var(--bg-surface-hover)';
                    e.currentTarget.style.color = 'var(--text-primary)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!active) {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = 'var(--text-secondary)';
                  }
                }}
              >
                <span
                  style={{
                    fontSize: '0.68rem',
                    fontFamily: 'var(--font-mono)',
                    color: active ? 'var(--accent-primary)' : 'var(--text-muted)',
                    fontWeight: 600,
                  }}
                >
                  {item.number}
                </span>
                <Icon size={16} strokeWidth={active ? 2.2 : 1.8} />
                <span style={{ flex: 1 }}>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Key Status & User Profile */}
        <div
          style={{
            marginTop: 'auto',
            paddingTop: '1.25rem',
            borderTop: 'var(--border-hairline)',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {/* Key Status Marker */}
          <div
            style={{
              padding: '0.55rem 0.75rem',
              border: hasKeys ? '1px solid var(--status-clean-border)' : '1px solid var(--status-warn-border)',
              background: hasKeys ? 'var(--status-clean-bg)' : 'var(--status-warn-bg)',
              borderRadius: 'var(--radius-sharp)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
              {hasKeys ? (
                <CheckCircle2 size={13} color="var(--status-clean)" />
              ) : (
                <AlertTriangle size={13} color="var(--status-warn)" />
              )}
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: hasKeys ? 'var(--status-clean)' : 'var(--status-warn)',
                }}
              >
                {hasKeys ? 'Keys Active' : 'Keys Unset'}
              </span>
            </div>
            <span
              style={{
                fontSize: '0.68rem',
                fontFamily: 'var(--font-mono)',
                color: 'var(--text-muted)',
              }}
            >
              RSA-4096
            </span>
          </div>

          {/* User Account Details */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.25rem 0',
            }}
          >
            <div style={{ overflow: 'hidden' }}>
              <div
                style={{
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  color: 'var(--text-primary)',
                  letterSpacing: '-0.01em',
                }}
              >
                {user?.username || 'Authenticated User'}
              </div>
              <div
                style={{
                  fontSize: '0.68rem',
                  color: 'var(--text-muted)',
                  fontFamily: 'var(--font-mono)',
                  textTransform: 'uppercase',
                }}
              >
                ROLE: {user?.role || 'USER'}
              </div>
            </div>
            <button
              onClick={logout}
              title="Log out of session"
              style={{
                background: 'transparent',
                border: 'var(--border-hairline)',
                color: 'var(--text-secondary)',
                cursor: 'pointer',
                padding: '0.45rem',
                borderRadius: 'var(--radius-sharp)',
                display: 'flex',
                alignItems: 'center',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = 'var(--status-danger-border)';
                e.currentTarget.style.color = 'var(--status-danger)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = 'var(--hairline)';
                e.currentTarget.style.color = 'var(--text-secondary)';
              }}
            >
              <LogOut size={14} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main style={{ flex: 1, padding: '2.5rem 3rem', overflowY: 'auto' }}>
        <div style={{ maxWidth: '1140px', margin: '0 auto' }}>{children}</div>
      </main>
    </div>
  );
}
