import { useState, FormEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { authApi } from '../api/authApi';
import { generateUserKeyPairBundle } from '../crypto/webCryptoService';
import { showToast } from '../components/Toast';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import {
  validateUsername,
  validateEmail,
  validatePassword,
  validatePasswordConfirmation,
} from '../utils/validators';
import { Shield, Lock, User, Mail, AlertCircle, Cpu, ArrowRight } from 'lucide-react';

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
    const userErr = validateUsername(username);
    if (userErr) {
      setError(userErr);
      return;
    }
    const emailErr = validateEmail(email);
    if (emailErr) {
      setError(emailErr);
      return;
    }
    const passErr = validatePassword(password);
    if (passErr) {
      setError(passErr);
      return;
    }
    const matchErr = validatePasswordConfirmation(password, confirmPassword);
    if (matchErr) {
      setError(matchErr);
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
        padding: '2.5rem 1.5rem',
        background: 'var(--bg-primary)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Ambient background glow */}
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

      <div style={{ width: '100%', maxWidth: '480px', position: 'relative', zIndex: 1 }}>
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
            Create Your Account
          </h1>
          <p
            style={{
              fontSize: '0.8125rem',
              color: 'var(--text-muted)',
              fontWeight: 500,
            }}
          >
            Client-side cryptographic key generation & zero-knowledge security
          </p>
        </div>

        <Card title="Register" subtitle="Zero-Knowledge Setup" className="card-glow">
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

            {stepStatus && (
              <div
                style={{
                  background: 'rgba(99, 102, 241, 0.12)',
                  border: '1px solid rgba(99, 102, 241, 0.35)',
                  color: 'var(--text-primary)',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                }}
              >
                <Cpu size={16} color="#818cf8" style={{ animation: 'pulseGlow 1.5s infinite' }} />
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.775rem' }}>{stepStatus}</span>
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
                Username
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. alice"
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
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="email"
                  className="form-input"
                  placeholder="alice@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ paddingLeft: '2.5rem' }}
                  autoComplete="email"
                />
                <Mail
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

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
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
                    placeholder="Min 8 chars"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={{ paddingLeft: '2.5rem' }}
                    autoComplete="new-password"
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
                  Confirm Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="password"
                    className="form-input"
                    placeholder="Re-enter password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    style={{ paddingLeft: '2.5rem' }}
                    autoComplete="new-password"
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
            </div>

            <Button
              type="submit"
              variant="primary"
              loading={loading}
              icon={<Shield size={16} />}
              style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem' }}
            >
              Generate Keys & Register
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
              <span>Already registered?</span>
              <button
                type="button"
                onClick={onNavigateToLogin}
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
                <span>Sign In</span>
                <ArrowRight size={13} />
              </button>
            </div>
          </form>
        </Card>
      </div>
    </div>
  );
}
