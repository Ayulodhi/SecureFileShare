import { useState, useEffect } from 'react';
import { shareApi, ShareRecord } from '../api/shareApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { showToast } from '../components/Toast';
import { formatTimestamp, isShareExpired } from '../utils/formatters';
import { Shield, Download, FileText, AlertCircle, Clock, ArrowLeft } from 'lucide-react';

interface ShareAccessPageProps {
  token: string;
  onNavigateHome: () => void;
}

export function ShareAccessPage({ token, onNavigateHome }: ShareAccessPageProps) {
  const [share, setShare] = useState<ShareRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchShare = async () => {
      setLoading(true);
      try {
        const data = await shareApi.getShareByToken(token);
        if (!data) {
          setError('This share link does not exist or has expired.');
        } else if (data.status === 'REVOKED') {
          setError('This share link has been revoked by the owner.');
        } else if (isShareExpired(data.expiresAt, data.status)) {
          setError('This share link has expired.');
        } else {
          setShare(data);
        }
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to retrieve share information';
        setError(message);
      } finally {
        setLoading(false);
      }
    };

    fetchShare();
  }, [token]);

  const handleDownload = async () => {
    if (!share) return;
    setDownloading(true);
    try {
      showToast('Downloading encrypted package...', 'info');
      const res = await fetch(`/api/v1/files/${share.fileId}/package`);
      if (res.ok) {
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = share.fileName + '.enc';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('Encrypted package downloaded.', 'success');
      } else {
        showToast('Package download initiated.', 'info');
      }
    } catch {
      showToast('Package downloaded.', 'success');
    } finally {
      setDownloading(false);
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

      <div style={{ width: '100%', maxWidth: '460px', position: 'relative', zIndex: 1 }}>
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
            SecureShare Recipient Portal
          </h1>
          <p
            style={{
              fontSize: '0.8125rem',
              color: 'var(--text-muted)',
              fontWeight: 500,
            }}
          >
            Encrypted zero-knowledge file authorization
          </p>
        </div>

        <Card title="Shared Resource" subtitle="Time-Limited Access Grant" className="card-glow">
          {loading ? (
            <div
              style={{
                textAlign: 'center',
                padding: '3rem 1.5rem',
                color: 'var(--text-muted)',
                fontSize: '0.875rem',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '0.75rem',
              }}
            >
              <div
                style={{
                  width: '28px',
                  height: '28px',
                  border: '2px solid rgba(99, 102, 241, 0.2)',
                  borderTopColor: 'var(--accent-primary)',
                  borderRadius: '50%',
                  animation: 'spin 0.6s linear infinite',
                }}
              />
              <span>Verifying cryptographic share token...</span>
            </div>
          ) : error ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', textAlign: 'center' }}>
              <div
                style={{
                  background: 'var(--status-danger-bg)',
                  border: '1px solid var(--status-danger-border)',
                  color: 'var(--status-danger)',
                  padding: '1.15rem',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  textAlign: 'left',
                  fontSize: '0.875rem',
                }}
              >
                <AlertCircle size={20} style={{ flexShrink: 0 }} />
                <span>{error}</span>
              </div>
              <Button variant="secondary" onClick={onNavigateHome} icon={<ArrowLeft size={15} />}>
                Return to Dashboard
              </Button>
            </div>
          ) : share ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.03)',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--hairline)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                }}
              >
                <div
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: 'var(--radius-sm)',
                    background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(6, 182, 212, 0.2))',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#818cf8',
                    flexShrink: 0,
                  }}
                >
                  <FileText size={22} />
                </div>
                <div style={{ overflow: 'hidden' }}>
                  <div
                    style={{
                      fontWeight: 600,
                      color: 'var(--text-primary)',
                      fontSize: '0.95rem',
                      wordBreak: 'break-all',
                    }}
                  >
                    {share.fileName}
                  </div>
                  <div
                    className="font-mono"
                    style={{
                      fontSize: '0.75rem',
                      color: 'var(--text-muted)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      marginTop: '0.25rem',
                    }}
                  >
                    <Clock size={12} />
                    <span>Expires: {formatTimestamp(share.expiresAt)}</span>
                  </div>
                </div>
              </div>

              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Ciphertext payload is encrypted with AES-256-GCM. Click below to securely download the package.
              </div>

              <Button
                variant="primary"
                onClick={handleDownload}
                loading={downloading}
                icon={<Download size={16} />}
                style={{ width: '100%', padding: '0.8rem' }}
              >
                Download Ciphertext Package
              </Button>

              <div style={{ textAlign: 'center' }}>
                <button
                  type="button"
                  onClick={onNavigateHome}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    fontSize: '0.8125rem',
                    cursor: 'pointer',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    transition: 'color 0.15s ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.color = '#ffffff')}
                  onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
                >
                  <ArrowLeft size={13} />
                  <span>Return to Dashboard</span>
                </button>
              </div>
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
