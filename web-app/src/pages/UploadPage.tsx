import { useState, useRef, DragEvent, ChangeEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { getUserKeys } from '../crypto/keyVault';
import { encryptFilePayload } from '../crypto/webCryptoService';
import { authApi } from '../api/authApi';
import { fileApi } from '../api/fileApi';
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
  Sparkles,
  FileCheck,
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
    }
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setError(null);
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
        recipientRsaPublicKey = recipientReg.rsaPublicKey;
      }

      setProgressStep('Generating AES-256 session key & encrypting payload...');
      const encryptedPackage = await encryptFilePayload(
        selectedFile,
        recipientRsaPublicKey,
        userKeys.ecPrivateKey
      );

      setProgressStep('Uploading encrypted ciphertext blob...');
      await fileApi.uploadEncryptedFile(
        encryptedPackage.ciphertextBlob,
        selectedFile.name,
        encryptedPackage.ivBase64,
        encryptedPackage.ecdsaSignatureBase64,
        encryptedPackage.ciphertextHashHex,
        encryptedPackage.ownerWrappedKeyBase64
      );

      showToast(`"${selectedFile.name}" encrypted and uploaded successfully.`, 'success');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      onUploadSuccess();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Upload failed.';
      setError(message);
    } finally {
      setEncrypting(false);
      setProgressStep(null);
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
            Client-Side Cryptography Pipeline
          </div>
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

          {/* GetShared-Inspired Interactive Drop Zone */}
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
              boxShadow: isDragging
                ? '0 0 35px rgba(99, 102, 241, 0.3)'
                : 'none',
              position: 'relative',
              overflow: 'hidden',
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
                  gap: '1rem',
                }}
              >
                <div
                  style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: 'var(--radius-lg)',
                    background: 'rgba(16, 185, 129, 0.15)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxShadow: '0 4px 20px rgba(16, 185, 129, 0.2)',
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
                      wordBreak: 'break-all',
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
                    <span
                      style={{
                        background: 'rgba(255, 255, 255, 0.06)',
                        padding: '0.2rem 0.6rem',
                        borderRadius: 'var(--radius-pill)',
                        color: 'var(--text-secondary)',
                      }}
                    >
                      {formatFileSize(selectedFile.size)}
                    </span>
                    <span>Ready for local encryption</span>
                  </div>
                </div>
                <div
                  style={{
                    fontSize: '0.75rem',
                    color: 'var(--accent-primary)',
                    fontWeight: 500,
                    marginTop: '0.25rem',
                  }}
                >
                  Click or drop another file to replace
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
                    boxShadow: '0 8px 24px rgba(99, 102, 241, 0.25)',
                    animation: 'floatSoft 3s ease-in-out infinite',
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

          {/* Segmented Option Selector: Key Wrapping Target */}
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
              {/* Option 1: Myself */}
              <div
                onClick={() => setIsSelf(true)}
                style={{
                  padding: '1rem 1.15rem',
                  borderRadius: 'var(--radius-md)',
                  border: isSelf
                    ? '1px solid var(--accent-primary)'
                    : '1px solid var(--hairline)',
                  background: isSelf
                    ? 'rgba(99, 102, 241, 0.1)'
                    : 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.85rem',
                  transition: 'all 0.2s ease',
                  boxShadow: isSelf ? '0 0 15px rgba(99, 102, 241, 0.2)' : 'none',
                }}
              >
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: isSelf
                      ? '5px solid var(--accent-primary)'
                      : '2px solid var(--text-muted)',
                    background: '#ffffff',
                    flexShrink: 0,
                    transition: 'all 0.2s ease',
                  }}
                />
                <div>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    Encrypt for Myself
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                    Wraps key with your RSA public key ({user?.username})
                  </div>
                </div>
              </div>

              {/* Option 2: Recipient User */}
              <div
                onClick={() => setIsSelf(false)}
                style={{
                  padding: '1rem 1.15rem',
                  borderRadius: 'var(--radius-md)',
                  border: !isSelf
                    ? '1px solid var(--accent-primary)'
                    : '1px solid var(--hairline)',
                  background: !isSelf
                    ? 'rgba(99, 102, 241, 0.1)'
                    : 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.85rem',
                  transition: 'all 0.2s ease',
                  boxShadow: !isSelf ? '0 0 15px rgba(99, 102, 241, 0.2)' : 'none',
                }}
              >
                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: !isSelf
                      ? '5px solid var(--accent-primary)'
                      : '2px solid var(--text-muted)',
                    background: '#ffffff',
                    flexShrink: 0,
                    transition: 'all 0.2s ease',
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
                  Recipient Username
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Enter registered recipient username (e.g. alice)"
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

          {/* Cryptographic Execution Status */}
          {progressStep && (
            <div
              style={{
                background: 'rgba(99, 102, 241, 0.12)',
                border: '1px solid rgba(99, 102, 241, 0.35)',
                color: 'var(--text-primary)',
                padding: '0.9rem 1.15rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                boxShadow: '0 0 20px rgba(99, 102, 241, 0.15)',
              }}
            >
              <Cpu size={18} color="#818cf8" style={{ animation: 'pulseGlow 1.5s infinite' }} />
              <span className="font-mono" style={{ fontSize: '0.8125rem' }}>
                {progressStep}
              </span>
            </div>
          )}

          {/* Footer Actions */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              alignItems: 'center',
              gap: '1rem',
              paddingTop: '0.5rem',
            }}
          >
            {selectedFile && (
              <Button
                variant="secondary"
                onClick={() => {
                  setSelectedFile(null);
                  if (fileInputRef.current) fileInputRef.current.value = '';
                }}
                disabled={encrypting}
              >
                Clear
              </Button>
            )}
            <Button
              variant="primary"
              onClick={handleUpload}
              disabled={!selectedFile || encrypting}
              loading={encrypting}
              icon={<Shield size={16} />}
              style={{ minWidth: '180px' }}
            >
              Encrypt & Upload
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
