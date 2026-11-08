import { useState, FormEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { authApi } from '../api/authApi';
import { generateUserKeyPairBundle } from '../crypto/webCryptoService';
import { hasUserKeys } from '../crypto/keyVault';
import { showToast } from '../components/Toast';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Shield, Lock, User, AlertCircle, Key, ArrowRight } from 'lucide-react';

interface LoginPageProps {
  onNavigateToRegister: () => void;
  onNavigateToKeySetup?: () => void;
}

export function LoginPage({ onNavigateToRegister }: LoginPageProps) {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Please enter your username and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const authRes = await authApi.login({
        usernameOrEmail: username.trim(),
        password,
      });

      // Check if user already has local keys in IndexedDB
      const keysExist = await hasUserKeys(authRes.username);
      if (!keysExist) {
        showToast('First login on this browser: generating cryptographic keys...', 'info');
        try {
          const newBundle = await generateUserKeyPairBundle(authRes.username);
          await authApi.registerKeys({
            rsaPublicKey: newBundle.rsaPublicKeyPem,
            ecPublicKey: newBundle.ecPublicKeyPem,
          });
          showToast('Cryptographic key pair registered successfully.', 'success');
        } catch (keyErr) {
          console.warn('Could not auto-register keys:', keyErr);
        }
      }

      login(
        {
          id: authRes.userId,
          username: authRes.username,
          email: authRes.email,
          role: authRes.role,
        },
        authRes.token
      );

      showToast(`Authenticated as ${authRes.username}.`, 'success');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Login failed. Please check credentials.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '2.5rem 1.5rem',
        background: 'var(--bg-primary)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Ambient background glow behind login card */}
      <div
        style={{
          position: 'absolute',
          width: '500px',
          height: '500px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%)',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          pointerEvents: 'none',
        }}
      />

      <div style={{ width: '100%', maxWidth: '440px', position: 'relative', zIndex: 1 }}>
        {/* Brand Header */}
        <div style={{ marginBottom: '2.25rem', textAlign: 'center' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              background: 'var(--accent-gradient)',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              boxShadow: '0 8px 24px rgba(99, 102, 241, 0.4), 0 0 30px rgba(6, 182, 212, 0.2)',
            }}
          >
            <Shield size={26} color="#ffffff" strokeWidth={2.2} />
          </div>
          <h1
            style={{
              fontSize: '1.5rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
              marginBottom: '0.35rem',
            }}
          >
            Welcome to SecureShare
          </h1>
          <p
            style={{
              fontSize: '0.8125rem',
              color: 'var(--text-muted)',
              fontWeight: 500,
            }}
          >
            Zero-knowledge, client-side encrypted workspace
          </p>
        </div>

        <Card title="Sign In" subtitle="Session Authentication" className="card-glow">
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {error && (
              <div
                style={{
                  background: 'var(--status-danger-bg)',
                  border: '1px solid var(--status-danger-border)',
                  color: 'var(--status-danger)',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.65rem',
                }}
              >
                <AlertCircle size={16} style={{ flexShrink: 0 }} />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  letterSpacing: '0.02em',
                  marginBottom: '0.45rem',
                  color: 'var(--text-secondary)',
                }}
              >
                Username or Email
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Enter your username or email"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  style={{ paddingLeft: '2.5rem' }}
                  autoComplete="username"
                />
                <User
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '0.9rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-muted)',
                  }}
                />
              </div>
            </div>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  letterSpacing: '0.02em',
                  marginBottom: '0.45rem',
                  color: 'var(--text-secondary)',
                }}
              >
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  className="form-input"
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ paddingLeft: '2.5rem' }}
                  autoComplete="current-password"
                />
                <Lock
                  size={16}
                  style={{
                    position: 'absolute',
                    left: '0.9rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-muted)',
                  }}
                />
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              icon={<Key size={15} />}
              style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem' }}
            >
              Sign In
            </Button>

            <div
              style={{
                fontSize: '0.8125rem',
                color: 'var(--text-secondary)',
                marginTop: '0.5rem',
                paddingTop: '1.25rem',
                borderTop: '1px solid var(--hairline)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <span>Need an account?</span>
              <button
                type="button"
                onClick={onNavigateToRegister}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#818cf8',
                  fontWeight: 600,
                  fontSize: '0.8125rem',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  padding: 0,
                  transition: 'color 0.15s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.color = '#a5b4fc')}
                onMouseLeave={(e) => (e.currentTarget.style.color = '#818cf8')}
              >
                <span>Register</span>
                <ArrowRight size={13} />
              </button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
