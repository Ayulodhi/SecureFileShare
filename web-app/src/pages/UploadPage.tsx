import { useState, useRef, DragEvent, ChangeEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { getUserKeys } from '../crypto/keyVault';
import { encryptFilePayload, arrayBufferToBase64 } from '../crypto/webCryptoService';
import { authApi } from '../api/authApi';
import { fileApi } from '../api/fileApi';
import { shareApi } from '../api/shareApi';
import { showToast } from '../components/Toast';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { formatFileSize } from '../utils/formatters';
import {
  UploadCloud,
  Shield,
  AlertCircle,
  User,
  Cpu,
  Lock,
  FileCheck,
  Download,
  Copy,
  Check,
} from 'lucide-react';

interface UploadPageProps {
  onUploadSuccess: () => void;
}

export function UploadPage({ onUploadSuccess }: UploadPageProps) {
  const { user } = useAuth();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [recipientUsername, setRecipientUsername] = useState('');
  const [isSelf, setIsSelf] = useState(true);
  const [isDragging, setIsDragging] = useState(false);
  const [encrypting, setEncrypting] = useState(false);
  const [progressStep, setProgressStep] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [localEncryptedResult, setLocalEncryptedResult] = useState<{
    ciphertextBase64: string;
    ivBase64: string;
    wrappedKeyBase64: string;
    sha256Hash: string;
  } | null>(null);
  const [copied, setCopied] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setSelectedFile(e.dataTransfer.files[0]);
      setError(null);
      setLocalEncryptedResult(null);
    }
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setError(null);
      setLocalEncryptedResult(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Select a file to encrypt and upload.');
      return;
    }
    if (!user) {
      setError('Active session required.');
      return;
    }

    setEncrypting(true);
    setError(null);

    try {
      setProgressStep('Accessing local cryptographic vault...');
      const userKeys = await getUserKeys(user.username);
      if (!userKeys) {
        throw new Error(
          'Private keys missing in this browser. Please visit Key Vault to generate or restore keys.'
        );
      }

      let recipientRsaPublicKey = userKeys.rsaPublicKeyPem;
      if (!isSelf && recipientUsername.trim()) {
        setProgressStep(`Resolving public key for "${recipientUsername.trim()}"...`);
        const recipientReg = await authApi.getKeyByUsername(recipientUsername.trim());
        if (!recipientReg || !recipientReg.rsaPublicKey || recipientReg.rsaPublicKey.trim() === '') {
          throw new Error(`Recipient "${recipientUsername.trim()}" has not registered public keys yet. Please ensure they have created an account and visited their Key Vault.`);
        }
        recipientRsaPublicKey = recipientReg.rsaPublicKey;
      }

      setProgressStep('Generating AES-256 session key & encrypting payload...');
      const encryptedPackage = await encryptFilePayload(
        selectedFile,
        recipientRsaPublicKey,
        userKeys.ecPrivateKey
      );

      // Generate local encrypted package for dedicated decrypt workspace download
      const ciphertextBase64 = arrayBufferToBase64(await encryptedPackage.ciphertextBlob.arrayBuffer());
      setLocalEncryptedResult({
        ciphertextBase64,
        ivBase64: encryptedPackage.ivBase64,
        wrappedKeyBase64: encryptedPackage.ownerWrappedKeyBase64,
        sha256Hash: encryptedPackage.ciphertextHashHex,
      });

      setProgressStep('Uploading encrypted ciphertext blob...');
      const uploadedFile = await fileApi.uploadEncryptedFile(
        encryptedPackage.ciphertextBlob,
        selectedFile.name,
        encryptedPackage.ivBase64,
        encryptedPackage.ecdsaSignatureBase64,
        encryptedPackage.ciphertextHashHex,
        encryptedPackage.ownerWrappedKeyBase64
      );

      if (!isSelf && recipientUsername.trim()) {
        setProgressStep(`Notifying recipient "${recipientUsername.trim()}" via email...`);
        try {
          await shareApi.createShare({
            fileId: uploadedFile.id,
            fileName: selectedFile.name,
            recipientUsername: recipientUsername.trim(),
            expiryOption: '24 hours',
          });
        } catch (shareErr) {
          console.error('Auto-share notification error:', shareErr);
        }
      }

      showToast(`"${selectedFile.name}" encrypted and uploaded successfully.`, 'success');
      onUploadSuccess();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Upload failed.';
      setError(message);
    } finally {
      setEncrypting(false);
      setProgressStep(null);
    }
  };

  const handleDownloadCiphertext = () => {
    if (!localEncryptedResult || !selectedFile) return;
    const jsonStr = JSON.stringify(localEncryptedResult, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${selectedFile.name}.encrypted-package.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast('Encrypted package downloaded successfully for local decrypt workspace.', 'success');
  };

  const handleCopyCiphertext = () => {
    if (!localEncryptedResult) return;
    navigator.clipboard.writeText(JSON.stringify(localEncryptedResult, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
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
          <h1
            style={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              color: 'var(--text-primary)',
            }}
          >
            Upload & Encrypt
          </h1>
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            background: 'rgba(99, 102, 241, 0.08)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            padding: '0.4rem 0.85rem',
            borderRadius: 'var(--radius-pill)',
            fontSize: '0.75rem',
            color: '#818cf8',
            fontWeight: 500,
          }}
        >
          <Lock size={12} />
          <span>Zero-Knowledge AES-256-GCM</span>
        </div>
      </div>

      {/* 3D Holographic Cyber Banner */}
      <div
        className="animate-float-3d"
        style={{
          background: 'linear-gradient(135deg, rgba(0, 240, 255, 0.1), rgba(255, 0, 110, 0.08))',
          border: '1px solid var(--accent-cyan)',
          borderRadius: 'var(--radius-lg)',
          padding: '1.25rem 1.75rem',
          display: 'flex',
          alignItems: 'center',
          gap: '1.25rem',
          boxShadow: '0 0 30px rgba(0, 240, 255, 0.2)',
        }}
      >
        <div
          className="animate-pulse-glow"
          style={{
            width: '44px',
            height: '44px',
            background: 'var(--accent-cyan)',
            color: '#05020D',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            fontWeight: 700,
          }}
        >
          <Lock size={22} />
        </div>
        <div>
          <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--accent-cyan)', marginBottom: '0.2rem' }}>
            Holographic Zero-Knowledge Ingress Active
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
            Files are encrypted locally on your device via AES-256-GCM before transmission. The server never sees plaintext.
          </p>
        </div>
      </div>

      <Card
        title="Encryption Ingress"
        subtitle="End-to-End Cryptographic Transfer"
        className="card-glow"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
          {error && (
            <div
              style={{
                background: 'var(--status-danger-bg)',
                border: '1px solid var(--status-danger-border)',
                color: 'var(--status-danger)',
                padding: '0.85rem 1.15rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.65rem',
              }}
            >
              <AlertCircle size={18} style={{ flexShrink: 0 }} />
              <span>{error}</span>
            </div>
          )}

          {/* Drop Zone */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            style={{
              border: isDragging
                ? '2px solid var(--accent-primary)'
                : selectedFile
                ? '1px solid rgba(16, 185, 129, 0.4)'
                : '1px dashed rgba(255, 255, 255, 0.2)',
              borderRadius: 'var(--radius-xl)',
              padding: selectedFile ? '2.5rem 2rem' : '3.5rem 2rem',
              textAlign: 'center',
              cursor: 'pointer',
              background: isDragging
                ? 'rgba(99, 102, 241, 0.15)'
                : selectedFile
                ? 'rgba(16, 185, 129, 0.04)'
                : 'rgba(255, 255, 255, 0.02)',
              transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
              position: 'relative',
            }}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />

            {selectedFile ? (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.85rem',
                }}
              >
                <div
                  className="animate-float-3d"
                  style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: 'var(--radius-lg)',
                    background: 'rgba(0, 240, 255, 0.15)',
                    border: '1px solid var(--accent-cyan)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxShadow: '0 0 20px rgba(0, 240, 255, 0.3)',
                  }}
                >
                  <FileCheck size={28} color="var(--status-clean)" />
                </div>
                <div>
                  <div
                    style={{
                      fontSize: '1.1rem',
                      fontWeight: 600,
                      color: 'var(--text-primary)',
                      marginBottom: '0.25rem',
                    }}
                  >
                    {selectedFile.name}
                  </div>
                  <div
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      fontSize: '0.8125rem',
                      color: 'var(--text-muted)',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    <span>{formatFileSize(selectedFile.size)}</span>
                    <span>Ready for local encryption</span>
                  </div>
                </div>
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                }}
              >
                <div
                  style={{
                    width: '64px',
                    height: '64px',
                    borderRadius: 'var(--radius-xl)',
                    background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(6, 182, 212, 0.15))',
                    border: '1px solid rgba(255, 255, 255, 0.12)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '1.25rem',
                  }}
                >
                  <UploadCloud size={30} color="#818cf8" />
                </div>
                <div
                  style={{
                    fontSize: '1.05rem',
                    fontWeight: 600,
                    color: 'var(--text-primary)',
                    marginBottom: '0.4rem',
                  }}
                >
                  Choose a file or drag & drop it here
                </div>
                <div
                  style={{
                    fontSize: '0.8125rem',
                    color: 'var(--text-muted)',
                    maxWidth: '420px',
                    lineHeight: 1.5,
                  }}
                >
                  Payload is encrypted locally in your browser with AES-256-GCM before transmission. Zero server access.
                </div>
              </div>
            )}
          </div>

          {/* Key Wrapping Target Selector */}
          <div
            style={{
              borderTop: '1px solid var(--hairline)',
              paddingTop: '1.5rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '1rem',
            }}
          >
            <div
              style={{
                fontSize: '0.75rem',
                fontWeight: 600,
                letterSpacing: '0.04em',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
              }}
            >
              Key Wrapping Target
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                gap: '0.85rem',
              }}
            >
              <div
                onClick={() => setIsSelf(true)}
                style={{
                  padding: '1rem 1.15rem',
                  borderRadius: 'var(--radius-md)',
                  border: isSelf ? '1px solid var(--accent-primary)' : '1px solid var(--hairline)',
                  background: isSelf ? 'rgba(99, 102, 241, 0.1)' : 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.85rem',
                }}
              >
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: isSelf ? '5px solid var(--accent-primary)' : '2px solid var(--text-muted)',
                    background: '#ffffff',
                    flexShrink: 0,
                  }}
                />
                <div>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Encrypt for Myself
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                    Wraps key with your RSA public key
                  </div>
                </div>
              </div>

              <div
                onClick={() => setIsSelf(false)}
                style={{
                  padding: '1rem 1.15rem',
                  borderRadius: 'var(--radius-md)',
                  border: !isSelf ? '1px solid var(--accent-primary)' : '1px solid var(--hairline)',
                  background: !isSelf ? 'rgba(99, 102, 241, 0.1)' : 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.85rem',
                }}
              >
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: !isSelf ? '5px solid var(--accent-primary)' : '2px solid var(--text-muted)',
                    background: '#ffffff',
                    flexShrink: 0,
                  }}
                />
                <div>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Encrypt for Specific Recipient
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                    Resolves recipient's verified RSA public key
                  </div>
                </div>
              </div>
            </div>

            {!isSelf && (
              <div style={{ marginTop: '0.5rem' }}>
                <label
                  style={{
                    display: 'block',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    marginBottom: '0.45rem',
                    color: 'var(--text-secondary)',
                  }}
                >
                  Recipient Email or Username
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="friend@example.com or username"
                    value={recipientUsername}
                    onChange={(e) => setRecipientUsername(e.target.value)}
                    style={{ paddingLeft: '2.5rem' }}
                  />
                  <User
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
            )}
          </div>

          {progressStep && (
            <div
              style={{
                background: 'rgba(99, 102, 241, 0.08)',
                border: '1px solid rgba(99, 102, 241, 0.25)',
                color: '#818cf8',
                padding: '0.85rem 1.15rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
              }}
            >
              <Cpu size={18} className="animate-spin" style={{ flexShrink: 0 }} />
              <span>{progressStep}</span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
            <Button
              variant="primary"
              onClick={handleUpload}
              loading={encrypting}
              icon={<Shield size={16} />}
              style={{ padding: '0.75rem 1.75rem' }}
            >
              {encrypting ? 'Encrypting & Uploading...' : 'Encrypt & Upload Securely'}
            </Button>
          </div>

          {/* Dedicated Local Encrypted Package Workspace Download (Fulfilling user request for offline/local decrypt workspace) */}
          {localEncryptedResult && (
            <div
              style={{
                marginTop: '1rem',
                padding: '1.25rem',
                background: 'rgba(16, 185, 129, 0.06)',
                border: '1px solid rgba(16, 185, 129, 0.3)',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.75rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#34d399', fontWeight: 600, fontSize: '0.9rem' }}>
                <Check size={16} /> Ciphertext Package Ready for Dedicated Decryption Workspace
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                You can download the raw JSON encrypted package below to store in your local encrypted file space or test offline decryption.
              </div>
              <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.25rem' }}>
                <Button
                  variant="secondary"
                  onClick={handleDownloadCiphertext}
                  icon={<Download size={14} />}
                  style={{ fontSize: '0.8rem', padding: '0.45rem 0.85rem' }}
                >
                  Download .encrypted-package.json
                </Button>
                <Button
                  variant="secondary"
                  onClick={handleCopyCiphertext}
                  icon={copied ? <Check size={14} /> : <Copy size={14} />}
                  style={{ fontSize: '0.8rem', padding: '0.45rem 0.85rem' }}
                >
                  {copied ? 'Copied Package JSON' : 'Copy Package JSON'}
                </Button>
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
