import { useState, useEffect } from 'react';
import { shareApi, ShareRecord } from '../api/shareApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Table, Column } from '../components/Table';
import { showToast } from '../components/Toast';
import { formatTimestamp, getStatusBadgeInfo } from '../utils/formatters';
import { Copy, Check, Trash2, RefreshCw, Share2, Sparkles, Clock, Link } from 'lucide-react';

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(99, 102, 241, 0.12)',
              border: '1px solid rgba(99, 102, 241, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#818cf8',
              flexShrink: 0,
            }}
          >
            <Link size={16} />
          </div>
          <div>
            <span style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.9rem' }}>
              {s.fileName}
            </span>
            <div className="font-mono" style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
              TOKEN: {s.id}
            </div>
          </div>
        </div>
      ),
    },
    {
      header: 'Recipient',
      accessor: (s) => (
        <span
          className="badge badge-neutral font-mono"
          style={{ fontSize: '0.75rem', padding: '0.25rem 0.65rem' }}
        >
          {s.recipientUsername || 'ANY'}
        </span>
      ),
      width: '140px',
    },
    {
      header: 'Status',
      accessor: (s) => {
        const badge = getStatusBadgeInfo(s.status);
        const isActive = badge.variant === 'success';
        return (
          <span className={`badge badge-${badge.variant}`}>
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                background: isActive ? 'var(--status-clean)' : 'var(--status-danger)',
                display: 'inline-block',
              }}
            />
            <span>{badge.label}</span>
          </span>
        );
      },
      width: '130px',
    },
    {
      header: 'Expires At',
      accessor: (s) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)' }}>
          <Clock size={12} />
          <span className="font-mono" style={{ fontSize: '0.775rem' }}>
            {formatTimestamp(s.expiresAt)}
          </span>
        </div>
      ),
      width: '190px',
    },
    {
      header: 'Actions',
      accessor: (s) => (
        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
          <Button
            variant="secondary"
            onClick={() => handleCopyLink(s)}
            icon={copiedId === s.id ? <Check size={13} /> : <Copy size={13} />}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.785rem' }}
          >
            {copiedId === s.id ? 'Copied' : 'Copy'}
          </Button>
          {s.status === 'ACTIVE' && (
            <Button
              variant="danger"
              onClick={() => handleRevoke(s.id)}
              icon={<Trash2 size={13} />}
              style={{ padding: '0.45rem 0.85rem', fontSize: '0.785rem' }}
            >
              Revoke
            </Button>
          )}
        </div>
      ),
      width: '190px',
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
          paddingBottom: '1.5rem',
          borderBottom: '1px solid var(--hairline)',
        }}
      >
        <div>
          <div className="subheading-kicker" style={{ marginBottom: '0.4rem' }}>
            <Sparkles size={13} />
            Access Delegation Control
          </div>
          <h1
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
            }}
          >
            Share Links
          </h1>
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

      <Card
        title="Active Authorizations"
        subtitle="Time-limited HMAC token grants"
        className="card-glow"
      >
        <Table
          columns={columns}
          data={shares}
          loading={loading}
          emptyMessage="No active share links recorded. Create one by clicking 'Share' on any file."
        />
      </Card>
    </div>
  );
}
