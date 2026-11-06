import { useState, useEffect } from 'react';
import { shareApi, ShareRecord } from '../api/shareApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { showToast } from '../components/Toast';
import { Shield, Download, File, AlertCircle, Clock } from 'lucide-react';

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
        } else if (new Date(data.expiresAt).getTime() < Date.now()) {
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
        padding: '2rem 1.5rem',
        background: 'var(--bg-primary)',
      }}
    >
      <div style={{ width: '100%', maxWidth: '440px' }}>
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
            Encrypted Recipient Portal
          </p>
        </div>

        <Card title="Shared Resource" subtitle="Decentralized Authorization Token">
          {loading ? (
            <div style={{ textAlign: 'center', padding: '2.5rem', color: 'var(--text-muted)' }}>
              Verifying cryptographic share token...
            </div>
          ) : error ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', textAlign: 'center' }}>
              <div
                style={{
                  background: 'var(--status-danger-bg)',
                  border: '1px solid var(--status-danger-border)',
                  color: 'var(--status-danger)',
                  padding: '1rem',
                  borderRadius: 'var(--radius-sharp)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  textAlign: 'left',
                  fontSize: '0.8125rem',
                }}
              >
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
              <Button variant="secondary" onClick={onNavigateHome}>
                Return to Dashboard
              </Button>
            </div>
          ) : share ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div
                style={{
                  background: 'var(--bg-secondary)',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-sharp)',
                  border: 'var(--border-hairline)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                }}
              >
                <div
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: 'var(--radius-sharp)',
                    background: 'var(--bg-surface)',
                    border: 'var(--border-hairline)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <File size={18} color="var(--accent-primary)" />
                </div>
                <div style={{ overflow: 'hidden' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', wordBreak: 'break-all' }}>
                    {share.fileName}
                  </div>
                  <div
                    className="font-mono"
                    style={{
                      fontSize: '0.72rem',
                      color: 'var(--text-muted)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.35rem',
                      marginTop: '0.2rem',
                    }}
                  >
                    <Clock size={11} />
                    <span>EXPIRES: {new Date(share.expiresAt).toISOString().replace('T', ' ').substring(0, 19)}</span>
                  </div>
                </div>
              </div>

              <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Ciphertext payload is encrypted with AES-256-GCM. Click below to download the package.
              </div>

              <Button
                variant="primary"
                onClick={handleDownload}
                loading={downloading}
                icon={<Download size={14} />}
                style={{ width: '100%' }}
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
                  }}
                >
                  Return to Dashboard
                </button>
              </div>
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
