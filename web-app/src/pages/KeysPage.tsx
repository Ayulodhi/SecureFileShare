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
import { Key, Shield, Download, Upload, Copy, Check, Lock, AlertTriangle, Sparkles, CheckCircle2 } from 'lucide-react';

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
          paddingBottom: '1.5rem',
          borderBottom: '1px solid var(--hairline)',
        }}
      >
        <div>
          <div className="subheading-kicker" style={{ marginBottom: '0.4rem' }}>
            <Sparkles size={13} />
            Zero-Knowledge Key Architecture
          </div>
          <h1
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
            }}
          >
            Cryptographic Vault
          </h1>
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
            padding: '1.5rem',
            borderRadius: 'var(--radius-lg)',
            background: 'rgba(245, 158, 11, 0.08)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: '44px',
                height: '44px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(245, 158, 11, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <AlertTriangle size={22} color="var(--status-warn)" />
            </div>
            <div>
              <div
                style={{
                  fontWeight: 700,
                  fontSize: '0.95rem',
                  letterSpacing: '-0.01em',
                  color: 'var(--status-warn)',
                }}
              >
                Zero-Knowledge Keys Not Found in Browser
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
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
        <Card
          title="Active Cryptographic Identity"
          subtitle="IndexedDB Local Store: sfs_vault"
          className="card-glow"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
            {/* Fingerprint Input Row */}
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
                Public Key Fingerprint (SHA-256)
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type="text"
                  readOnly
                  className="form-input font-mono"
                  value={keys.fingerprint}
                  style={{ fontSize: '0.8125rem', background: 'rgba(0, 0, 0, 0.35)' }}
                />
                <Button
                  variant="secondary"
                  onClick={handleCopyFingerprint}
                  icon={copiedFingerprint ? <Check size={14} /> : <Copy size={14} />}
                >
                  {copiedFingerprint ? 'Copied' : 'Copy'}
                </Button>
              </div>
            </div>

            {/* Cryptographic Pair Cards */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
                gap: '1.25rem',
              }}
            >
              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.025)',
                  padding: '1.5rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--hairline)',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '1rem' }}>
                  <div
                    style={{
                      width: '32px',
                      height: '32px',
                      borderRadius: 'var(--radius-sm)',
                      background: 'rgba(99, 102, 241, 0.15)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#818cf8',
                    }}
                  >
                    <Shield size={18} />
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                      RSA-OAEP 4096-bit
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Asymmetric Key Wrapping</div>
                  </div>
                </div>
                <div
                  className="font-mono"
                  style={{
                    fontSize: '0.75rem',
                    color: 'var(--text-secondary)',
                    lineHeight: 1.8,
                    background: 'rgba(0, 0, 0, 0.2)',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--hairline)',
                  }}
                >
                  <div>USAGE: DEK Encapsulation (OAEP)</div>
                  <div>DIGEST: SHA-256</div>
                  <div>EXPONENT: 65537 (0x10001)</div>
                </div>
              </div>

              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.025)',
                  padding: '1.5rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--hairline)',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '1rem' }}>
                  <div
                    style={{
                      width: '32px',
                      height: '32px',
                      borderRadius: 'var(--radius-sm)',
                      background: 'rgba(6, 182, 212, 0.15)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#06b6d4',
                    }}
                  >
                    <Key size={18} />
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                      ECDSA P-256
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Digital Signature Verification</div>
                  </div>
                </div>
                <div
                  className="font-mono"
                  style={{
                    fontSize: '0.75rem',
                    color: 'var(--text-secondary)',
                    lineHeight: 1.8,
                    background: 'rgba(0, 0, 0, 0.2)',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--hairline)',
                  }}
                >
                  <div>USAGE: Digital Signature Verification</div>
                  <div>CURVE: NIST P-256 (secp256r1)</div>
                  <div>STORAGE: Non-extractable CryptoKey</div>
                </div>
              </div>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-start',
                paddingTop: '1rem',
                borderTop: '1px solid var(--hairline)',
              }}
            >
              <Button variant="danger" onClick={handleRegenerateKeys} icon={<AlertTriangle size={14} />}>
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
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
              Your private keys will be encrypted client-side with <strong>PBKDF2 (100,000 iterations)</strong> + <strong>AES-256-GCM</strong> using your passphrase before export.
            </p>

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
                Passphrase for Backup
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter strong passphrase"
                  value={exportPassphrase}
                  onChange={(e) => setExportPassphrase(e.target.value)}
                  style={{ paddingLeft: '2.5rem' }}
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

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button variant="secondary" onClick={() => setIsExportModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleExportBackup}
                loading={exportLoading}
                icon={<Download size={14} />}
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
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
              Paste your previously exported encrypted backup JSON to restore your private keys into this browser.
            </p>

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
                Backup JSON
              </label>
              <textarea
                className="form-input font-mono"
                rows={5}
                placeholder='{"username": "...", "salt": "...", "ciphertext": "..."}'
                value={importJson}
                onChange={(e) => setImportJson(e.target.value)}
                style={{ fontSize: '0.775rem', resize: 'vertical' }}
              />
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
                Decryption Passphrase
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  className="form-input"
                  placeholder="Enter the passphrase used during export"
                  value={importPassphrase}
                  onChange={(e) => setImportPassphrase(e.target.value)}
                  style={{ paddingLeft: '2.5rem' }}
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

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button variant="secondary" onClick={() => setIsImportModalOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleImportBackup}
                loading={importLoading}
                icon={<Upload size={14} />}
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
