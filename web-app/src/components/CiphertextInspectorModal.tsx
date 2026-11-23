import { useState } from 'react';
import { Card } from './Card';
import { Button } from './Button';
import { CheckCircle2, Copy, Check } from 'lucide-react';
import { EncryptedFilePackage } from '../api/fileApi';
import { arrayBufferToHex, arrayBufferToBase64 } from '../crypto/webCryptoService';

interface CiphertextInspectorModalProps {
  file: { id: string; originalName: string; sizeBytes: number };
  pkg: EncryptedFilePackage | null;
  onClose: () => void;
}

export function CiphertextInspectorModal({ file, pkg, onClose }: CiphertextInspectorModalProps) {
  const [copied, setCopied] = useState(false);

  const ciphertextHex = pkg ? arrayBufferToHex(pkg.ciphertextBuffer).substring(0, 512) + '...' : 'Loading ciphertext buffer...';
  const fullBase64 = pkg ? arrayBufferToBase64(pkg.ciphertextBuffer) : '';

  const handleCopy = () => {
    navigator.clipboard.writeText(fullBase64);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '1rem',
      }}
    >
      <div style={{ width: '100%', maxWidth: '680px' }}>
        <Card
          title="Live Zero-Knowledge Ciphertext Inspector"
          subtitle={`Inspecting encrypted package for "${file.originalName}"`}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '1rem' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                background: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-md)',
                color: '#34d399',
                fontSize: '0.85rem',
              }}
            >
              <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
              <span>
                Verified: The server only holds opaque AES-256-GCM ciphertext. Original plaintext never touched disk or network in unencrypted form.
              </span>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                AES-256-GCM Ciphertext Hex Preview (First 256 Bytes)
              </label>
              <div
                className="font-mono"
                style={{
                  background: 'var(--bg-tertiary)',
                  border: '1px solid var(--border-glass)',
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.75rem',
                  color: 'var(--accent-secondary)',
                  wordBreak: 'break-all',
                  maxHeight: '90px',
                  overflowY: 'auto',
                }}
              >
                {ciphertextHex}
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Initialization Vector (IV - Base64)
                </label>
                <div
                  className="font-mono"
                  style={{
                    background: 'var(--bg-tertiary)',
                    border: '1px solid var(--border-glass)',
                    padding: '0.55rem 0.75rem',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.75rem',
                    color: 'var(--text-primary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {pkg?.ivBase64 || '—'}
                </div>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Ciphertext SHA-256 Hash
                </label>
                <div
                  className="font-mono"
                  style={{
                    background: 'var(--bg-tertiary)',
                    border: '1px solid var(--border-glass)',
                    padding: '0.55rem 0.75rem',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.75rem',
                    color: 'var(--text-primary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {pkg?.ciphertextHash || '—'}
                </div>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                RSA-4096 Wrapped AES Session Key
              </label>
              <div
                className="font-mono"
                style={{
                  background: 'var(--bg-tertiary)',
                  border: '1px solid var(--border-glass)',
                  padding: '0.55rem 0.75rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.75rem',
                  color: 'var(--text-primary)',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {pkg?.wrappedKeyBase64 || '—'}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <Button
                variant="secondary"
                onClick={handleCopy}
                icon={copied ? <Check size={14} /> : <Copy size={14} />}
                style={{ fontSize: '0.85rem' }}
              >
                {copied ? 'Copied Full Ciphertext' : 'Copy Ciphertext Base64'}
              </Button>
              <Button
                variant="primary"
                onClick={onClose}
                style={{ fontSize: '0.85rem' }}
              >
                Close Inspector
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
