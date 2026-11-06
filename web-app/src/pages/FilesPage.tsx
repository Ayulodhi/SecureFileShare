import { useState, useEffect } from 'react';
import { useAuth } from '../state/AuthContext';
import { fileApi, FileMetadataResponse } from '../api/fileApi';
import { getUserKeys } from '../crypto/keyVault';
import { decryptFilePayload } from '../crypto/webCryptoService';
import { showToast } from '../components/Toast';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Table, Column } from '../components/Table';
import { ShareModal } from '../components/ShareModal';
import {
  Download,
  Share2,
  RefreshCw,
  FileCheck,
  ShieldAlert,
  Clock,
  File,
  Plus,
} from 'lucide-react';

interface FilesPageProps {
  onNavigateToUpload: () => void;
}

export function FilesPage({ onNavigateToUpload }: FilesPageProps) {
  const { user } = useAuth();
  const [files, setFiles] = useState<FileMetadataResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [decryptingFileId, setDecryptingFileId] = useState<string | null>(null);
  const [shareTarget, setShareTarget] = useState<FileMetadataResponse | null>(null);

  const loadFiles = async () => {
    setLoading(true);
    try {
      const data = await fileApi.listFiles();
      setFiles(data || []);
    } catch (err: unknown) {
      console.warn('Could not load files:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFiles();
  }, []);

  const handleDownloadAndDecrypt = async (file: FileMetadataResponse) => {
    if (!user) return;
    setDecryptingFileId(file.id);

    try {
      showToast(`Fetching ciphertext for "${file.originalName}"...`, 'info');
      const pkg = await fileApi.downloadFilePackage(file.id);

      showToast(`Decrypting with local RSA-4096 private key...`, 'info');
      const userKeys = await getUserKeys(user.username);
      if (!userKeys) {
        throw new Error('Local private key missing from IndexedDB.');
      }

      const plaintextBuffer = await decryptFilePayload(
        pkg.ciphertextBuffer,
        pkg.ivBase64,
        pkg.wrappedKeyBase64,
        userKeys.rsaPrivateKey
      );

      const blob = new Blob([plaintextBuffer]);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.originalName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      showToast(`"${file.originalName}" decrypted and saved.`, 'success');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Decryption failed';
      showToast(message, 'error');
    } finally {
      setDecryptingFileId(null);
    }
  };

  const columns: Column<FileMetadataResponse>[] = [
    {
      header: 'File Name',
      accessor: (f) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              width: '24px',
              height: '24px',
              border: 'var(--border-hairline)',
              background: 'var(--bg-secondary)',
              borderRadius: 'var(--radius-sharp)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <File size={13} color="var(--accent-primary)" />
          </div>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{f.originalName}</div>
            <div
              style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}
              className="font-mono"
            >
              SHA-256: {f.ciphertextSha256 ? f.ciphertextSha256.substring(0, 16) + '...' : 'Verified'}
            </div>
          </div>
        </div>
      ),
    },
    {
      header: 'Ciphertext Size',
      accessor: (f) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
          {(f.sizeBytes / 1024).toFixed(1)} KB
        </span>
      ),
      width: '130px',
    },
    {
      header: 'Integrity',
      accessor: (f) => {
        const isClean = f.status === 'CLEAN';
        const isPending = f.status === 'PENDING_SCAN';
        return (
          <span
            className={`badge ${isClean ? 'badge-success' : isPending ? 'badge-warning' : 'badge-danger'}`}
          >
            {isClean ? <FileCheck size={11} /> : isPending ? <Clock size={11} /> : <ShieldAlert size={11} />}
            <span>{f.status}</span>
          </span>
        );
      },
      width: '130px',
    },
    {
      header: 'Timestamp',
      accessor: (f) => (
        <span className="font-mono" style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
          {new Date(f.createdAt).toISOString().replace('T', ' ').substring(0, 19)}
        </span>
      ),
      width: '180px',
    },
    {
      header: 'Actions',
      accessor: (f) => (
        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
          <Button
            variant="secondary"
            onClick={() => handleDownloadAndDecrypt(f)}
            loading={decryptingFileId === f.id}
            icon={<Download size={13} />}
            style={{ padding: '0.45rem 0.75rem', fontSize: '0.75rem' }}
          >
            Decrypt
          </Button>
          <Button
            variant="primary"
            onClick={() => setShareTarget(f)}
            icon={<Share2 size={13} />}
            style={{ padding: '0.45rem 0.75rem', fontSize: '0.75rem' }}
          >
            Share
          </Button>
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
          paddingBottom: '1.25rem',
          borderBottom: 'var(--border-hairline)',
        }}
      >
        <div>
          <div className="subheading-swiss" style={{ marginBottom: '0.35rem' }}>
            Zero-Knowledge Repository
          </div>
          <h2
            style={{
              fontSize: '1.5rem',
              fontWeight: 700,
              letterSpacing: '-0.02em',
              color: 'var(--text-primary)',
            }}
          >
            Encrypted Files
          </h2>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="secondary"
            onClick={loadFiles}
            loading={loading}
            icon={<RefreshCw size={14} />}
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            onClick={onNavigateToUpload}
            icon={<Plus size={14} />}
          >
            Upload File
          </Button>
        </div>
      </div>

      {/* Files Table */}
      <Card title="Repository Inventory" subtitle="Payloads stored as AES-256-GCM ciphertexts">
        <Table
          columns={columns}
          data={files}
          loading={loading}
          emptyMessage="No encrypted files present in repository."
        />
      </Card>

      {/* Share Modal Dialog */}
      {shareTarget && (
        <ShareModal
          file={shareTarget}
          onClose={() => setShareTarget(null)}
        />
      )}
    </div>
  );
}
