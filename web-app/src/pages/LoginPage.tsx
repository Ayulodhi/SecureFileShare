import { useState, FormEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { authApi } from '../api/authApi';
import { generateUserKeyPairBundle } from '../crypto/webCryptoService';
import { hasUserKeys } from '../crypto/keyVault';
import { showToast } from '../components/Toast';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Shield, Lock, User, AlertCircle, Key } from 'lucide-react';

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
        padding: '2rem 1.5rem',
        background: 'var(--bg-primary)',
      }}
    >
      <div style={{ width: '100%', maxWidth: '440px' }}>
        {/* Swiss Brand Header */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                background: 'var(--accent-primary)',
                borderRadius: 'var(--radius-sharp)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Shield size={18} color="#ffffff" strokeWidth={2.5} />
            </div>
            <h1
              style={{
                fontSize: '1.25rem',
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
              fontSize: '0.72rem',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              fontWeight: 600,
            }}
          >
            Zero-Knowledge Client Portal
          </p>
        </div>

        <Card title="Sign In" subtitle="Session Authentication">
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {error && (
              <div
                style={{
                  background: 'var(--status-danger-bg)',
                  border: '1px solid var(--status-danger-border)',
                  color: 'var(--status-danger)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-sharp)',
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <AlertCircle size={15} />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  marginBottom: '0.4rem',
                  color: 'var(--text-secondary)',
                }}
              >
                Username or Email
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. admin"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  style={{ paddingLeft: '2.4rem' }}
                  autoComplete="username"
                />
                <User
                  size={15}
                  style={{
                    position: 'absolute',
                    left: '0.85rem',
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
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  marginBottom: '0.4rem',
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
                  style={{ paddingLeft: '2.4rem' }}
                  autoComplete="current-password"
                />
                <Lock
                  size={15}
                  style={{
                    position: 'absolute',
                    left: '0.85rem',
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
              style={{ width: '100%', marginTop: '0.5rem' }}
            >
              Sign In
            </Button>

            <div
              style={{
                textAlign: 'left',
                fontSize: '0.8125rem',
                color: 'var(--text-secondary)',
                marginTop: '0.5rem',
                paddingTop: '1rem',
                borderTop: 'var(--border-hairline)',
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
                  color: 'var(--accent-primary)',
                  fontWeight: 600,
                  fontSize: '0.8125rem',
                  letterSpacing: '0.02em',
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                Register Account →
              </button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
