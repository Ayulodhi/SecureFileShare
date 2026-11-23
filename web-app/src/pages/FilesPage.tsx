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
import { CiphertextInspectorModal } from '../components/CiphertextInspectorModal';
import { EncryptedFilePackage } from '../api/fileApi';
import {
  formatFileSize,
  formatHash,
  formatTimestamp,
  getStatusBadgeInfo,
} from '../utils/formatters';
import {
  Download,
  Share2,
  RefreshCw,
  FileCheck,
  ShieldAlert,
  Clock,
  FileText,
  Plus,
  Sparkles,
  ShieldCheck,
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
  const [inspectTarget, setInspectTarget] = useState<FileMetadataResponse | null>(null);
  const [inspectPkg, setInspectPkg] = useState<EncryptedFilePackage | null>(null);
  const [inspecting, setInspecting] = useState(false);

  const handleInspect = async (file: FileMetadataResponse) => {
    setInspecting(true);
    try {
      const pkg = await fileApi.downloadFilePackage(file.id);
      setInspectPkg(pkg);
      setInspectTarget(file);
    } catch (err: unknown) {
      showToast('Failed to load ciphertext package for inspection.', 'error');
    } finally {
      setInspecting(false);
    }
  };

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(6, 182, 212, 0.1))',
              borderRadius: 'var(--radius-sm)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#818cf8',
              flexShrink: 0,
            }}
          >
            <FileText size={18} />
          </div>
          <div style={{ overflow: 'hidden' }}>
            <div
              style={{
                fontWeight: 600,
                color: 'var(--text-primary)',
                fontSize: '0.9rem',
                whiteSpace: 'nowrap',
                textOverflow: 'ellipsis',
                overflow: 'hidden',
              }}
            >
              {f.originalName}
            </div>
            <div
              style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}
              className="font-mono"
            >
              SHA-256: {formatHash(f.ciphertextSha256)}
            </div>
          </div>
        </div>
      ),
    },
    {
      header: 'Ciphertext Size',
      accessor: (f) => (
        <span
          className="font-mono"
          style={{
            color: 'var(--text-secondary)',
            fontSize: '0.8125rem',
            background: 'rgba(255, 255, 255, 0.04)',
            padding: '0.2rem 0.55rem',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--hairline)',
          }}
        >
          {formatFileSize(f.sizeBytes)}
        </span>
      ),
      width: '140px',
    },
    {
      header: 'Integrity',
      accessor: (f) => {
        const badge = getStatusBadgeInfo(f.status);
        const isClean = badge.variant === 'success';
        const isPending = badge.variant === 'warning';
        return (
          <span className={`badge badge-${badge.variant}`}>
            {isClean ? <FileCheck size={12} /> : isPending ? <Clock size={12} /> : <ShieldAlert size={12} />}
            <span>{badge.label}</span>
          </span>
        );
      },
      width: '140px',
    },
    {
      header: 'Created',
      accessor: (f) => (
        <span className="font-mono" style={{ color: 'var(--text-muted)', fontSize: '0.775rem' }}>
          {formatTimestamp(f.createdAt)}
        </span>
      ),
      width: '170px',
    },
    {
      header: 'Actions',
      accessor: (f) => (
        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
          <Button
            variant="secondary"
            onClick={() => handleInspect(f)}
            loading={inspecting && inspectTarget?.id === f.id}
            icon={<ShieldCheck size={13} />}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.785rem' }}
          >
            Inspect
          </Button>
          <Button
            variant="secondary"
            onClick={() => handleDownloadAndDecrypt(f)}
            loading={decryptingFileId === f.id}
            icon={<Download size={13} />}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.785rem' }}
          >
            Decrypt
          </Button>
          <Button
            variant="primary"
            onClick={() => setShareTarget(f)}
            icon={<Share2 size={13} />}
            style={{ padding: '0.45rem 0.85rem', fontSize: '0.785rem' }}
          >
            Share
          </Button>
        </div>
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
          paddingBottom: '1.5rem',
          borderBottom: '1px solid var(--hairline)',
        }}
      >
        <div>
          <div className="subheading-kicker" style={{ marginBottom: '0.4rem' }}>
            <Sparkles size={13} />
            Zero-Knowledge Repository
          </div>
          <h1
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
            }}
          >
            Encrypted Files
          </h1>
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
            icon={<Plus size={15} />}
          >
            Upload File
          </Button>
        </div>
      </div>

      {/* Files Table Card */}
      <Card
        title="Repository Inventory"
        subtitle="Payloads encrypted with client AES-256-GCM"
        className="card-glow"
      >
        <Table
          columns={columns}
          data={files}
          loading={loading}
          emptyMessage="No encrypted files found in repository. Click 'Upload File' to get started."
        />
      </Card>

      {/* Share Modal Dialog */}
      {shareTarget && (
        <ShareModal
          file={shareTarget}
          onClose={() => setShareTarget(null)}
        />
      )}

      {/* Ciphertext Inspector Modal */}
      {inspectTarget && (
        <CiphertextInspectorModal
          file={inspectTarget}
          pkg={inspectPkg}
          onClose={() => {
            setInspectTarget(null);
            setInspectPkg(null);
          }}
        />
      )}
    </div>
  );
}
