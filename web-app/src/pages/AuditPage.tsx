import { useState, useEffect } from 'react';
import { auditApi, AuditEntry, AuditVerifyResult } from '../api/auditApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Table, Column } from '../components/Table';
import { showToast } from '../components/Toast';
import { formatTimestamp, formatHash } from '../utils/formatters';
import { ShieldCheck, ShieldAlert, RefreshCw, CheckCircle2, Sparkles, Link2 } from 'lucide-react';

export function AuditPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [verifyResult, setVerifyResult] = useState<AuditVerifyResult | null>(null);

  const loadEntries = async () => {
    setLoading(true);
    try {
      const data = await auditApi.getAuditEntries(0, 50);
      setEntries(data || []);
    } catch (err: unknown) {
      console.warn('Could not load audit entries:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEntries();
  }, []);

  const handleVerifyChain = async () => {
    setVerifying(true);
    try {
      const res = await auditApi.verifyChain();
      setVerifyResult(res);
      if (res.valid) {
        showToast(res.message, 'success');
      } else {
        showToast(res.message, 'error');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Verification failed';
      showToast(message, 'error');
    } finally {
      setVerifying(false);
    }
  };

  const columns: Column<AuditEntry>[] = [
    {
      header: 'Seq #',
      accessor: (e) => (
        <span
          className="font-mono"
          style={{
            color: 'var(--text-muted)',
            fontSize: '0.8125rem',
            background: 'rgba(255, 255, 255, 0.04)',
            padding: '0.15rem 0.5rem',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--hairline)',
          }}
        >
          #{e.id.toString().padStart(4, '0')}
        </span>
      ),
      width: '90px',
    },
    {
      header: 'Timestamp',
      accessor: (e) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)', fontSize: '0.775rem' }}>
          {formatTimestamp(e.timestamp)}
        </span>
      ),
      width: '180px',
    },
    {
      header: 'Actor',
      accessor: (e) => (
        <span style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.875rem' }}>
          {e.actor}
        </span>
      ),
      width: '130px',
    },
    {
      header: 'Action',
      accessor: (e) => {
        const isUpload = e.action === 'UPLOAD';
        const isDownload = e.action === 'DOWNLOAD';
        return (
          <span
            className="badge"
            style={{
              background: isUpload
                ? 'rgba(99, 102, 241, 0.15)'
                : isDownload
                ? 'var(--status-clean-bg)'
                : 'rgba(255, 255, 255, 0.05)',
              color: isUpload
                ? '#818cf8'
                : isDownload
                ? 'var(--status-clean)'
                : 'var(--text-secondary)',
              borderColor: isUpload
                ? 'rgba(99, 102, 241, 0.3)'
                : isDownload
                ? 'var(--status-clean-border)'
                : 'var(--hairline)',
              fontSize: '0.725rem',
              fontWeight: 600,
            }}
          >
            {e.action}
          </span>
        );
      },
      width: '140px',
    },
    {
      header: 'Resource',
      accessor: (e) => (
        <span style={{ color: 'var(--text-primary)', fontWeight: 500, fontSize: '0.875rem' }}>
          {e.file || '—'}
        </span>
      ),
    },
    {
      header: 'Hash Chain Proof (SHA-256)',
      accessor: (e) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Link2 size={12} color="var(--accent-primary)" />
          <span
            className="font-mono"
            style={{
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              background: 'rgba(0, 0, 0, 0.25)',
              padding: '0.15rem 0.45rem',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            {e.hash ? formatHash(e.hash, 18) : 'GENESIS'}
          </span>
        </div>
      ),
      width: '210px',
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
            Cryptographic Integrity Ledger
          </div>
          <h1
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
            }}
          >
            Audit Trail
          </h1>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="secondary"
            onClick={loadEntries}
            loading={loading}
            icon={<RefreshCw size={14} />}
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            onClick={handleVerifyChain}
            loading={verifying}
            icon={<ShieldCheck size={16} />}
          >
            Verify Hash Chain
          </Button>
        </div>
      </div>

      {/* Verification Result Banner */}
      {verifyResult && (
        <div
          style={{
            padding: '1.25rem 1.5rem',
            background: verifyResult.valid
              ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(16, 185, 129, 0.05))'
              : 'linear-gradient(135deg, rgba(244, 63, 94, 0.12), rgba(244, 63, 94, 0.05))',
            border: `1px solid ${
              verifyResult.valid ? 'rgba(16, 185, 129, 0.35)' : 'rgba(244, 63, 94, 0.35)'
            }`,
            borderRadius: 'var(--radius-lg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: verifyResult.valid
              ? '0 0 25px rgba(16, 185, 129, 0.15)'
              : '0 0 25px rgba(244, 63, 94, 0.15)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: 'var(--radius-md)',
                background: verifyResult.valid
                  ? 'rgba(16, 185, 129, 0.2)'
                  : 'rgba(244, 63, 94, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {verifyResult.valid ? (
                <CheckCircle2 size={22} color="var(--status-clean)" />
              ) : (
                <ShieldAlert size={22} color="var(--status-danger)" />
              )}
            </div>
            <div>
              <div
                style={{
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  letterSpacing: '-0.01em',
                  color: verifyResult.valid ? 'var(--status-clean)' : 'var(--status-danger)',
                }}
              >
                {verifyResult.valid ? 'IMMUTABLE HASH CHAIN VERIFIED' : 'TAMPER DETECTED IN AUDIT LEDGER'}
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                {verifyResult.message}
              </div>
            </div>
          </div>
          <span
            className="badge font-mono"
            style={{
              background: 'rgba(255, 255, 255, 0.06)',
              color: 'var(--text-primary)',
              border: '1px solid var(--hairline-strong)',
              fontSize: '0.75rem',
              padding: '0.3rem 0.75rem',
            }}
          >
            {verifyResult.checkedCount} ENTRIES CHECKED
          </span>
        </div>
      )}

      {/* Ledger Table */}
      <Card
        title="Ledger Entries"
        subtitle="Append-only cryptographic hash chain linking blocks"
        className="card-glow"
      >
        <Table
          columns={columns}
          data={entries}
          loading={loading}
          emptyMessage="No audit records registered in the ledger."
        />
      </Card>
    </div>
  );
}
