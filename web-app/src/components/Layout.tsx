import { ReactNode } from 'react';
import { useAuth } from '../state/AuthContext';
import {
  Shield,
  UploadCloud,
  Share2,
  FileText,
  Key,
  LogOut,
  User,
  Users,
  Unlock,
} from 'lucide-react';

export type NavTab = 'files' | 'upload' | 'shares' | 'friends' | 'audit' | 'keys' | 'decrypt';

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
    { id: 'friends', label: 'Friends & Invites', icon: Users, number: '04' },
    { id: 'decrypt', label: 'Decrypt Workbench', icon: Unlock, number: '05' },
    { id: 'audit', label: 'Audit Trail', icon: Shield, number: '06' },
    { id: 'keys', label: 'Key Vault', icon: Key, number: '07' },
  ] as const;

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Sleek Glassmorphic Sidebar */}
      <aside
        style={{
          width: '280px',
          background: 'var(--bg-secondary)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          borderRight: 'var(--border-glass)',
          display: 'flex',
          flexDirection: 'column',
          padding: '1.75rem 1.25rem',
          flexShrink: 0,
          position: 'sticky',
          top: 0,
          height: '100vh',
          zIndex: 10,
        }}
      >
        {/* Brand Header */}
        <div style={{ marginBottom: '2.25rem', padding: '0 0.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.4rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                background: 'var(--accent-gradient)',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 14px rgba(99, 102, 241, 0.4), 0 0 20px rgba(6, 182, 212, 0.25)',
              }}
            >
              <Shield size={20} color="#ffffff" strokeWidth={2.2} />
            </div>
            <div>
              <div
                style={{
                  fontSize: '1.05rem',
                  fontWeight: 700,
                  letterSpacing: '-0.025em',
                  color: 'var(--text-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                }}
              >
                SecureShare
                <span
                  style={{
                    fontSize: '0.625rem',
                    background: 'rgba(99, 102, 241, 0.18)',
                    color: '#818cf8',
                    padding: '0.1rem 0.45rem',
                    borderRadius: 'var(--radius-pill)',
                    fontWeight: 600,
                    border: '1px solid rgba(99, 102, 241, 0.3)',
                  }}
                >
                  E2EE
                </span>
              </div>
              <p
                style={{
                  fontSize: '0.7rem',
                  color: 'var(--text-muted)',
                  fontWeight: 500,
                }}
              >
                Zero-Knowledge Vault
              </p>
            </div>
          </div>
        </div>

        {/* Section Label */}
        <div
          style={{
            fontSize: '0.6875rem',
            fontWeight: 600,
            letterSpacing: '0.06em',
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
            marginBottom: '0.6rem',
            paddingLeft: '0.75rem',
          }}
        >
          Navigation
        </div>

        {/* Navigation Tabs */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', flex: 1 }}>
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
                  padding: '0.7rem 0.85rem',
                  borderRadius: 'var(--radius-md)',
                  background: active ? 'rgba(99, 102, 241, 0.12)' : 'transparent',
                  color: active ? '#ffffff' : 'var(--text-secondary)',
                  border: active
                    ? '1px solid rgba(99, 102, 241, 0.3)'
                    : '1px solid transparent',
                  cursor: 'pointer',
                  fontSize: '0.875rem',
                  fontWeight: active ? 600 : 500,
                  transition: 'all 0.18s ease',
                  textAlign: 'left',
                  position: 'relative',
                  boxShadow: active ? '0 2px 10px rgba(99, 102, 241, 0.15)' : 'none',
                }}
                onMouseEnter={(e) => {
                  if (!active) {
                    e.currentTarget.style.background = 'rgba(255, 255, 255, 0.04)';
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
                <div
                  style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: 'var(--radius-sm)',
                    background: active ? 'rgba(99, 102, 241, 0.2)' : 'rgba(255, 255, 255, 0.03)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: active ? '#818cf8' : 'var(--text-muted)',
                    transition: 'all 0.18s ease',
                  }}
                >
                  <Icon size={16} strokeWidth={active ? 2.2 : 1.8} />
                </div>
                <span style={{ flex: 1 }}>{item.label}</span>
                {active && (
                  <span
                    style={{
                      width: '6px',
                      height: '6px',
                      borderRadius: '50%',
                      background: 'var(--accent-primary)',
                      boxShadow: '0 0 8px var(--accent-primary)',
                    }}
                  />
                )}
              </button>
            );
          })}
        </nav>

        {/* Security Status & User Profile */}
        <div
          style={{
            marginTop: 'auto',
            paddingTop: '1.25rem',
            borderTop: '1px solid var(--hairline)',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {/* Key Status Pill Card */}
          <div
            style={{
              padding: '0.75rem 0.9rem',
              border: hasKeys
                ? '1px solid rgba(16, 185, 129, 0.25)'
                : '1px solid rgba(245, 158, 11, 0.25)',
              background: hasKeys
                ? 'rgba(16, 185, 129, 0.08)'
                : 'rgba(245, 158, 11, 0.08)',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backdropFilter: 'blur(8px)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div
                style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  background: hasKeys ? 'var(--status-clean)' : 'var(--status-warn)',
                  boxShadow: hasKeys
                    ? '0 0 10px var(--status-clean)'
                    : '0 0 10px var(--status-warn)',
                  animation: 'pulseGlow 2.5s infinite ease-in-out',
                }}
              />
              <span
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: hasKeys ? 'var(--status-clean)' : 'var(--status-warn)',
                }}
              >
                {hasKeys ? 'Keys Active' : 'Keys Unset'}
              </span>
            </div>
            <span
              style={{
                fontSize: '0.6875rem',
                fontFamily: 'var(--font-mono)',
                color: 'var(--text-muted)',
                background: 'rgba(0, 0, 0, 0.2)',
                padding: '0.15rem 0.4rem',
                borderRadius: 'var(--radius-sm)',
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
              padding: '0.5rem 0.5rem 0.25rem',
              borderRadius: 'var(--radius-md)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', overflow: 'hidden' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(6, 182, 212, 0.2))',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#818cf8',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                }}
              >
                {user?.username ? user.username.charAt(0).toUpperCase() : <User size={15} />}
              </div>
              <div style={{ overflow: 'hidden' }}>
                <div
                  style={{
                    fontSize: '0.85rem',
                    fontWeight: 600,
                    color: 'var(--text-primary)',
                    letterSpacing: '-0.01em',
                    whiteSpace: 'nowrap',
                    textOverflow: 'ellipsis',
                    overflow: 'hidden',
                  }}
                >
                  {user?.username || 'Authenticated User'}
                </div>
                <div
                  style={{
                    fontSize: '0.6875rem',
                    color: 'var(--text-muted)',
                    fontFamily: 'var(--font-mono)',
                    textTransform: 'uppercase',
                  }}
                >
                  {user?.role || 'USER'}
                </div>
              </div>
            </div>
            <button
              onClick={logout}
              title="Log out of session"
              style={{
                background: 'rgba(255, 255, 255, 0.04)',
                border: '1px solid var(--hairline)',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                padding: '0.5rem',
                borderRadius: 'var(--radius-sm)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.18s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = 'var(--status-danger-border)';
                e.currentTarget.style.color = 'var(--status-danger)';
                e.currentTarget.style.background = 'var(--status-danger-bg)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = 'var(--hairline)';
                e.currentTarget.style.color = 'var(--text-muted)';
                e.currentTarget.style.background = 'rgba(255, 255, 255, 0.04)';
              }}
            >
              <LogOut size={15} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main
        style={{
          flex: 1,
          padding: '2.5rem 3.5rem',
          overflowY: 'auto',
          position: 'relative',
        }}
      >
        <div style={{ maxWidth: '1160px', margin: '0 auto' }}>{children}</div>
      </main>
    </div>
  );
}
