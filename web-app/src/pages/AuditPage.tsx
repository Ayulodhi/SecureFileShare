import { useState, useEffect } from 'react';
import { auditApi, AuditEntry, AuditVerifyResult } from '../api/auditApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Table, Column } from '../components/Table';
import { showToast } from '../components/Toast';
import { ShieldCheck, ShieldAlert, RefreshCw, CheckCircle2 } from 'lucide-react';

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
      accessor: (e) => <span className="font-mono" style={{ color: 'var(--text-muted)' }}>#{e.id.toString().padStart(4, '0')}</span>,
      width: '80px',
    },
    {
      header: 'Timestamp',
      accessor: (e) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
          {new Date(e.timestamp).toISOString().replace('T', ' ').substring(0, 19)}
        </span>
      ),
      width: '180px',
    },
    {
      header: 'Actor',
      accessor: (e) => <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{e.actor}</span>,
      width: '120px',
    },
    {
      header: 'Action',
      accessor: (e) => (
        <span
          className="badge"
          style={{
            background:
              e.action === 'UPLOAD'
                ? 'var(--accent-tint)'
                : e.action === 'DOWNLOAD'
                ? 'var(--status-clean-bg)'
                : 'var(--bg-secondary)',
            color:
              e.action === 'UPLOAD'
                ? 'var(--accent-primary)'
                : e.action === 'DOWNLOAD'
                ? 'var(--status-clean)'
                : 'var(--text-secondary)',
            borderColor:
              e.action === 'UPLOAD'
                ? 'var(--accent-primary)'
                : e.action === 'DOWNLOAD'
                ? 'var(--status-clean-border)'
                : 'var(--hairline)',
          }}
        >
          {e.action}
        </span>
      ),
      width: '140px',
    },
    {
      header: 'Resource',
      accessor: (e) => <span style={{ color: 'var(--text-primary)' }}>{e.file || '—'}</span>,
    },
    {
      header: 'Hash Chain Proof (SHA-256)',
      accessor: (e) => (
        <span className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
          {e.hash ? e.hash.substring(0, 18) + '...' : 'GENESIS'}
        </span>
      ),
      width: '200px',
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
            Cryptographic Integrity Ledger
          </div>
          <h2
            style={{
              fontSize: '1.5rem',
              fontWeight: 700,
              letterSpacing: '-0.02em',
              color: 'var(--text-primary)',
            }}
          >
            Audit Trail
          </h2>
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
            icon={<ShieldCheck size={14} />}
          >
            Verify Hash Chain
          </Button>
        </div>
      </div>

      {/* Verification Result Banner */}
      {verifyResult && (
        <div
          style={{
            padding: '1rem 1.25rem',
            background: verifyResult.valid ? 'var(--status-clean-bg)' : 'var(--status-danger-bg)',
            border: `1px solid ${verifyResult.valid ? 'var(--status-clean-border)' : 'var(--status-danger-border)'}`,
            borderRadius: 'var(--radius-sharp)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {verifyResult.valid ? (
              <CheckCircle2 size={18} color="var(--status-clean)" />
            ) : (
              <ShieldAlert size={18} color="var(--status-danger)" />
            )}
            <div>
              <div
                style={{
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  letterSpacing: '0.02em',
                  color: verifyResult.valid ? 'var(--status-clean)' : 'var(--status-danger)',
                }}
              >
                {verifyResult.valid ? 'IMMUTABLE HASH CHAIN VERIFIED' : 'TAMPER DETECTED IN AUDIT LEDGER'}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                {verifyResult.message}
              </div>
            </div>
          </div>
          <span className="badge badge-neutral font-mono">
            {verifyResult.checkedCount} ENTRIES CHECKED
          </span>
        </div>
      )}

      {/* Ledger Table */}
      <Card title="Ledger Entries" subtitle="Append-only hash chain linking previous hashes">
        <Table
          columns={columns}
          data={entries}
          loading={loading}
          emptyMessage="No audit records registered."
        />
      </Card>
    </div>
  );
}
