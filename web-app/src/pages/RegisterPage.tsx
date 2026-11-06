import { useState, FormEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { authApi } from '../api/authApi';
import { generateUserKeyPairBundle } from '../crypto/webCryptoService';
import { showToast } from '../components/Toast';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { Shield, Lock, User, Mail, AlertCircle, Cpu } from 'lucide-react';

interface RegisterPageProps {
  onNavigateToLogin: () => void;
}

export function RegisterPage({ onNavigateToLogin }: RegisterPageProps) {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [stepStatus, setStepStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      setStepStatus('Creating user credentials...');
      await authApi.register({
        username: username.trim(),
        email: email.trim(),
        password,
      });

      setStepStatus('Authenticating session...');
      const authRes = await authApi.login({
        usernameOrEmail: username.trim(),
        password,
      });

      setStepStatus('Generating RSA-4096 & ECDSA P-256 keys in browser...');
      const keyBundle = await generateUserKeyPairBundle(username.trim());

      setStepStatus('Registering public keys with directory...');
      await authApi.registerKeys({
        rsaPublicKey: keyBundle.rsaPublicKeyPem,
        ecPublicKey: keyBundle.ecPublicKeyPem,
      });

      login(
        {
          id: authRes.userId,
          username: authRes.username,
          email: authRes.email,
          role: authRes.role,
        },
        authRes.token
      );

      showToast('Account registered and cryptographic keys provisioned.', 'success');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Registration failed.';
      setError(message);
    } finally {
      setLoading(false);
      setStepStatus(null);
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
      <div style={{ width: '100%', maxWidth: '460px' }}>
        {/* Brand Header */}
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
            Zero-Knowledge Key Provisioning
          </p>
        </div>

        <Card title="Register Account" subtitle="Client-Side Key Generation">
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

            {stepStatus && (
              <div
                style={{
                  background: 'var(--accent-tint)',
                  border: '1px solid var(--accent-primary)',
                  color: 'var(--text-primary)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-sharp)',
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.65rem',
                }}
              >
                <Cpu size={15} color="var(--accent-primary)" />
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem' }}>{stepStatus}</span>
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
                Username
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. alice"
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
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="email"
                  className="form-input"
                  placeholder="alice@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ paddingLeft: '2.4rem' }}
                  autoComplete="email"
                />
                <Mail
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

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
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
                    placeholder="Min 8 chars"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={{ paddingLeft: '2.4rem' }}
                    autoComplete="new-password"
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
                  Confirm Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="password"
                    className="form-input"
                    placeholder="Re-enter password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    style={{ paddingLeft: '2.4rem' }}
                    autoComplete="new-password"
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
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              icon={<Shield size={15} />}
              style={{ width: '100%', marginTop: '0.5rem' }}
            >
              Generate Keys & Register
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
              <span>Already registered?</span>
              <button
                type="button"
                onClick={onNavigateToLogin}
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
                Sign In →
              </button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
