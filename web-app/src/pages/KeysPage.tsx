import { useState, useEffect } from 'react';
import { useAuth } from '../state/AuthContext';
import { getUserKeys, StoredUserKeys } from '../crypto/keyVault';
import {
  generateUserKeyPairBundle,
  exportEncryptedKeyBackup,
  importEncryptedKeyBackup,
} from '../crypto/webCryptoService';
import { authApi } from '../api/authApi';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Modal } from '../components/Modal';
import { showToast } from '../components/Toast';
import { Key, Shield, Download, Upload, Copy, Check, Lock, AlertTriangle } from 'lucide-react';

export function KeysPage() {
  const { user, refreshKeyStatus } = useAuth();
  const [keys, setKeys] = useState<StoredUserKeys | null>(null);
  const [loading, setLoading] = useState(false);
  const [copiedFingerprint, setCopiedFingerprint] = useState(false);

  // Backup Modal State
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [exportPassphrase, setExportPassphrase] = useState('');
  const [exportLoading, setExportLoading] = useState(false);

  // Restore Modal State
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [importJson, setImportJson] = useState('');
  const [importPassphrase, setImportPassphrase] = useState('');
  const [importLoading, setImportLoading] = useState(false);

  const loadKeys = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const stored = await getUserKeys(user.username);
      setKeys(stored);
    } catch (err: unknown) {
      console.warn('Could not read user keys:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadKeys();
  }, [user]);

  const handleCopyFingerprint = () => {
    if (keys?.fingerprint) {
      navigator.clipboard.writeText(keys.fingerprint);
      setCopiedFingerprint(true);
      showToast('Key fingerprint copied to clipboard.', 'info');
      setTimeout(() => setCopiedFingerprint(false), 2000);
    }
  };

  const handleRegenerateKeys = async () => {
    if (!user) return;
    if (
      !window.confirm(
        'Warning: Generating new keys will replace existing keys in this browser. You will not be able to decrypt files encrypted with old keys without a backup. Continue?'
      )
    ) {
      return;
    }

    setLoading(true);
    try {
      const bundle = await generateUserKeyPairBundle(user.username);
      await authApi.registerKeys({
        rsaPublicKey: bundle.rsaPublicKeyPem,
        ecPublicKey: bundle.ecPublicKeyPem,
      });
      setKeys(bundle);
      await refreshKeyStatus();
      showToast('New RSA-4096 & ECDSA P-256 keys generated.', 'success');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to generate keys';
      showToast(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleExportBackup = async () => {
    if (!keys || !exportPassphrase) {
      showToast('Enter a strong backup passphrase.', 'error');
      return;
    }

    setExportLoading(true);
    try {
      const backupString = await exportEncryptedKeyBackup(keys, exportPassphrase);
      const blob = new Blob([backupString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `sfs-key-backup-${keys.username}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      showToast('Key backup exported successfully.', 'success');
      setIsExportModalOpen(false);
      setExportPassphrase('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Backup export failed';
      showToast(message, 'error');
    } finally {
      setExportLoading(false);
    }
  };

  const handleImportBackup = async () => {
    if (!importJson || !importPassphrase) {
      showToast('Provide both the backup JSON and passphrase.', 'error');
      return;
    }

    setImportLoading(true);
    try {
      const restored = await importEncryptedKeyBackup(importJson, importPassphrase);
      await authApi.registerKeys({
        rsaPublicKey: restored.rsaPublicKeyPem,
        ecPublicKey: restored.ecPublicKeyPem,
      });

      setKeys(restored);
      await refreshKeyStatus();
      showToast('Keys restored into browser vault.', 'success');
      setIsImportModalOpen(false);
      setImportJson('');
      setImportPassphrase('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to restore backup (check passphrase)';
      showToast(message, 'error');
    } finally {
      setImportLoading(false);
    }
  };

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
            Zero-Knowledge Key Architecture
          </div>
          <h2
            style={{
              fontSize: '1.5rem',
              fontWeight: 700,
              letterSpacing: '-0.02em',
              color: 'var(--text-primary)',
            }}
          >
            Cryptographic Vault
          </h2>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="secondary"
            onClick={() => setIsImportModalOpen(true)}
            icon={<Upload size={14} />}
          >
            Import Backup
          </Button>
          <Button
            variant="primary"
            onClick={() => setIsExportModalOpen(true)}
            disabled={!keys}
            icon={<Download size={14} />}
          >
            Export Backup
          </Button>
        </div>
      </div>

      {/* Missing Keys Warning */}
      {!keys && !loading && (
        <div
          style={{
            padding: '1.25rem',
            borderRadius: 'var(--radius-sharp)',
            background: 'var(--status-warn-bg)',
            border: '1px solid var(--status-warn-border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertTriangle size={20} color="var(--status-warn)" />
            <div>
              <div
                style={{
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  letterSpacing: '0.02em',
                  textTransform: 'uppercase',
                  color: 'var(--status-warn)',
                }}
              >
                Zero-Knowledge Keys Not Found in Browser
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                Files cannot be encrypted or decrypted on this device until keys are generated or restored.
              </div>
            </div>
          </div>
          <Button variant="primary" onClick={handleRegenerateKeys} icon={<Key size={14} />}>
            Generate Key Pair
          </Button>
        </div>
      )}

      {/* Active Key Details Card */}
      {keys && (
        <Card title="Active Cryptographic Identity" subtitle="IndexedDB Key Store: sfs_vault">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.4rem',
                }}
              >
                Public Key Fingerprint (SHA-256)
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type="text"
                  readOnly
                  className="form-input font-mono"
                  value={keys.fingerprint}
                  style={{ fontSize: '0.8rem', background: 'var(--bg-input)' }}
                />
                <Button
                  variant="secondary"
                  onClick={handleCopyFingerprint}
                  icon={copiedFingerprint ? <Check size={13} /> : <Copy size={13} />}
                >
                  {copiedFingerprint ? 'Copied' : 'Copy'}
                </Button>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
              <div
                style={{
                  background: 'var(--bg-secondary)',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-sharp)',
                  border: 'var(--border-hairline)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <Shield size={16} color="var(--accent-primary)" />
                  <span style={{ fontWeight: 700, fontSize: '0.85rem' }}>RSA-OAEP 4096-bit</span>
                </div>
                <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                  <div>USAGE: Key Encapsulation (DEK Wrapping)</div>
                  <div>DIGEST: SHA-256</div>
                  <div>EXPONENT: 65537 (0x10001)</div>
                </div>
              </div>

              <div
                style={{
                  background: 'var(--bg-secondary)',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-sharp)',
                  border: 'var(--border-hairline)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                  <Key size={16} color="var(--accent-primary)" />
                  <span style={{ fontWeight: 700, fontSize: '0.85rem' }}>ECDSA P-256</span>
                </div>
                <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                  <div>USAGE: Digital Signature Verification</div>
                  <div>CURVE: NIST P-256 (secp256r1)</div>
                  <div>STORAGE: Non-extractable CryptoKey</div>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-start', paddingTop: '0.75rem', borderTop: 'var(--border-hairline)' }}>
              <Button variant="danger" onClick={handleRegenerateKeys} icon={<AlertTriangle size={13} />}>
                Regenerate Key Pair
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Export Backup Modal */}
      {isExportModalOpen && (
        <Modal title="Export Key Backup" onClose={() => setIsExportModalOpen(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
              Your private keys will be encrypted client-side with <strong>PBKDF2 (100,000 iterations)</strong> + <strong>AES-256-GCM</strong> using your passphrase before export.
            </p>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.4rem',
                }}
              >
                Passphrase for Backup
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter strong passphrase"
                  value={exportPassphrase}
                  onChange={(e) => setExportPassphrase(e.target.value)}
                  style={{ paddingLeft: '2.4rem' }}
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

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button variant="secondary" onClick={() => setIsExportModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleExportBackup}
                loading={exportLoading}
                icon={<Download size={13} />}
              >
                Download Encrypted JSON
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Import Backup Modal */}
      {isImportModalOpen && (
        <Modal title="Restore Key Backup" onClose={() => setIsImportModalOpen(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
              Paste your previously exported encrypted backup JSON to restore your private keys into this browser.
            </p>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.4rem',
                }}
              >
                Backup JSON
              </label>
              <textarea
                className="form-input font-mono"
                rows={5}
                placeholder='{"username": "...", "salt": "...", "ciphertext": "..."}'
                value={importJson}
                onChange={(e) => setImportJson(e.target.value)}
                style={{ fontSize: '0.75rem', resize: 'vertical' }}
              />
            </div>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '0.4rem',
                }}
              >
                Decryption Passphrase
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter the passphrase used during export"
                  value={importPassphrase}
                  onChange={(e) => setImportPassphrase(e.target.value)}
                  style={{ paddingLeft: '2.4rem' }}
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

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button variant="secondary" onClick={() => setIsImportModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleImportBackup}
                loading={importLoading}
                icon={<Upload size={13} />}
              >
                Restore Keys
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
