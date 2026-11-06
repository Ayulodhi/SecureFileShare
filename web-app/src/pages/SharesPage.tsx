import { useState, useEffect } from 'react';
import { shareApi, ShareRecord } from '../api/shareApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Table, Column } from '../components/Table';
import { showToast } from '../components/Toast';
import { Copy, Check, Trash2, RefreshCw } from 'lucide-react';

export function SharesPage() {
  const [shares, setShares] = useState<ShareRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const loadShares = async () => {
    setLoading(true);
    try {
      const data = await shareApi.getSharesCreated();
      setShares(data || []);
    } catch (err: unknown) {
      console.warn('Could not load shares:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadShares();
  }, []);

  const handleCopyLink = (share: ShareRecord) => {
    navigator.clipboard.writeText(share.shareLink);
    setCopiedId(share.id);
    showToast('Share link copied to clipboard.', 'info');
    setTimeout(() => setCopiedId(null), 2500);
  };

  const handleRevoke = async (shareId: string) => {
    try {
      await shareApi.revokeShare(shareId);
      showToast('Share link revoked successfully.', 'success');
      loadShares();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to revoke share';
      showToast(message, 'error');
    }
  };

  const columns: Column<ShareRecord>[] = [
    {
      header: 'File Name',
      accessor: (s) => (
        <div>
          <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{s.fileName}</span>
          <div className="font-mono" style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            TOKEN: {s.id}
          </div>
        </div>
      ),
    },
    {
      header: 'Recipient',
      accessor: (s) => (
        <span className="badge badge-neutral font-mono">{s.recipientUsername || 'ANY'}</span>
      ),
      width: '130px',
    },
    {
      header: 'Status',
      accessor: (s) => {
        const isActive = s.status === 'ACTIVE';
        return (
          <span className={`badge ${isActive ? 'badge-success' : 'badge-danger'}`}>
            {s.status}
          </span>
        );
      },
      width: '110px',
    },
    {
      header: 'Expires At',
      accessor: (s) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
          {new Date(s.expiresAt).toISOString().replace('T', ' ').substring(0, 19)}
        </span>
      ),
      width: '180px',
    },
    {
      header: 'Actions',
      accessor: (s) => (
        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
          <Button
            variant="secondary"
            onClick={() => handleCopyLink(s)}
            icon={copiedId === s.id ? <Check size={13} /> : <Copy size={13} />}
            style={{ padding: '0.45rem 0.75rem', fontSize: '0.75rem' }}
          >
            {copiedId === s.id ? 'Copied' : 'Copy'}
          </Button>
          {s.status === 'ACTIVE' && (
            <Button
              variant="danger"
              onClick={() => handleRevoke(s.id)}
              icon={<Trash2 size={13} />}
              style={{ padding: '0.45rem 0.75rem', fontSize: '0.75rem' }}
            >
              Revoke
            </Button>
          )}
        </div>
      ),
      width: '180px',
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Page Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
          paddingBottom: '1.25rem',
          borderBottom: 'var(--border-hairline)',
        }}
      >
        <div>
          <div className="subheading-swiss" style={{ marginBottom: '0.35rem' }}>
            Access Delegation Control
          </div>
          <h2
            style={{
              fontSize: '1.5rem',
              fontWeight: 700,
              letterSpacing: '-0.02em',
              color: 'var(--text-primary)',
            }}
          >
            Share Links
          </h2>
        </div>

        <Button
          variant="secondary"
          onClick={loadShares}
          loading={loading}
          icon={<RefreshCw size={14} />}
        >
          Refresh
        </Button>
      </div>

      <Card title="Active Authorizations" subtitle="Time-limited HMAC token grants">
        <Table
          columns={columns}
          data={shares}
          loading={loading}
          emptyMessage="No active share links recorded."
        />
      </Card>
    </div>
  );
}
