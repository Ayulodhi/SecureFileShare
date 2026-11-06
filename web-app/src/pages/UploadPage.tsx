import { useState, useRef, DragEvent, ChangeEvent } from 'react';
import { useAuth } from '../state/AuthContext';
import { getUserKeys } from '../crypto/keyVault';
import { encryptFilePayload } from '../crypto/webCryptoService';
import { authApi } from '../api/authApi';
import { fileApi } from '../api/fileApi';
import { showToast } from '../components/Toast';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import {
  UploadCloud,
  File,
  Shield,
  AlertCircle,
  User,
  Cpu,
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
      <div style={{ paddingBottom: '1.25rem', borderBottom: 'var(--border-hairline)' }}>
        <div className="subheading-swiss" style={{ marginBottom: '0.35rem' }}>
          Client-Side Cryptography Pipeline
        </div>
        <h2
          style={{
            fontSize: '1.5rem',
            fontWeight: 700,
            letterSpacing: '-0.02em',
            color: 'var(--text-primary)',
          }}
        >
          Upload & Encrypt
        </h2>
      </div>

      <Card title="Encryption Ingress" subtitle="AES-256-GCM + RSA-4096 Key Wrapping">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {error && (
            <div
              style={{
                background: 'var(--status-danger-bg)',
                border: '1px solid var(--status-danger-border)',
                color: 'var(--status-danger)',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-sharp)',
                fontSize: '0.8125rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <AlertCircle size={15} />
              <span>{error}</span>
            </div>
          )}

          {/* Drag & Drop Hairline Target */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            style={{
              border: isDragging ? '1px solid var(--accent-primary)' : '1px dashed var(--hairline-strong)',
              borderRadius: 'var(--radius-sharp)',
              padding: '3rem 1.5rem',
              textAlign: 'center',
              cursor: 'pointer',
              background: isDragging ? 'var(--accent-tint)' : 'var(--bg-secondary)',
              transition: 'all 0.12s ease',
            }}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: 'var(--radius-sharp)',
                background: 'var(--bg-surface)',
                border: 'var(--border-hairline)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1rem',
              }}
            >
              <UploadCloud size={20} color="var(--accent-primary)" />
            </div>

            {selectedFile ? (
              <div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600 }}>
                  <File size={16} color="var(--accent-primary)" />
                  <span style={{ fontSize: '0.9rem' }}>{selectedFile.name}</span>
                </div>
                <div
                  className="font-mono"
                  style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}
                >
                  {(selectedFile.size / 1024).toFixed(1)} KB — Click or drop to replace
                </div>
              </div>
            ) : (
              <div>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Drop plaintext file here or click to browse
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                  Payload is encrypted with AES-256-GCM before exiting this browser.
                </div>
              </div>
            )}
          </div>

          {/* Recipient Encryption Binding */}
          <div
            style={{
              borderTop: 'var(--border-hairline)',
              paddingTop: '1.25rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '1rem',
            }}
          >
            <label
              style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
              }}
            >
              Key Wrapping Target
            </label>

            <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontSize: '0.8125rem',
                  cursor: 'pointer',
                  color: isSelf ? 'var(--text-primary)' : 'var(--text-secondary)',
                }}
              >
                <input
                  type="radio"
                  name="recipientType"
                  checked={isSelf}
                  onChange={() => setIsSelf(true)}
                  style={{ accentColor: 'var(--accent-primary)' }}
                />
                <span>Encrypt for Myself ({user?.username})</span>
              </label>

              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontSize: '0.8125rem',
                  cursor: 'pointer',
                  color: !isSelf ? 'var(--text-primary)' : 'var(--text-secondary)',
                }}
              >
                <input
                  type="radio"
                  name="recipientType"
                  checked={!isSelf}
                  onChange={() => setIsSelf(false)}
                  style={{ accentColor: 'var(--accent-primary)' }}
                />
                <span>Encrypt for Recipient User</span>
              </label>
            </div>

            {!isSelf && (
              <div style={{ marginTop: '0.5rem' }}>
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Recipient username (e.g. alice)"
                    value={recipientUsername}
                    onChange={(e) => setRecipientUsername(e.target.value)}
                    style={{ paddingLeft: '2.4rem' }}
                  />
                  <User
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
            )}
          </div>

          {/* Cryptographic Execution Status */}
          {progressStep && (
            <div
              style={{
                background: 'var(--accent-tint)',
                border: '1px solid var(--accent-primary)',
                color: 'var(--text-primary)',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-sharp)',
                fontSize: '0.8125rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.65rem',
              }}
            >
              <Cpu size={15} color="var(--accent-primary)" />
              <span className="font-mono" style={{ fontSize: '0.75rem' }}>
                {progressStep}
              </span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <Button
              variant="primary"
              onClick={handleUpload}
              disabled={!selectedFile || encrypting}
              loading={encrypting}
              icon={<Shield size={14} />}
            >
              Encrypt & Upload
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
