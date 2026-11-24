import { useState } from 'react';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { showToast } from '../components/Toast';
import { base64ToArrayBuffer } from '../crypto/webCryptoService';
import { Key, Unlock, FileText, Upload, ShieldCheck, AlertCircle, Code } from 'lucide-react';

export function DecryptWorkbenchPage() {
  const [encryptedFile, setEncryptedFile] = useState<File | null>(null);
  const [ivBase64, setIvBase64] = useState('');
  const [wrappedKeyBase64, setWrappedKeyBase64] = useState('');
  const [originalName, setOriginalName] = useState('');
  const [jsonMetadataInput, setJsonMetadataInput] = useState('');
  const [decryptedBlobUrl, setDecryptedBlobUrl] = useState<string | null>(null);
  const [decryptedFileName, setDecryptedFileName] = useState<string>('');
  const [decrypting, setDecrypting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setEncryptedFile(file);
      if (!originalName) {
        setOriginalName(file.name.endsWith('.enc') ? file.name.slice(0, -4) : file.name + '.decrypted');
      }
    }
  };

  const handleJsonMetadataChange = (val: string) => {
    setJsonMetadataInput(val);
    try {
      const data = JSON.parse(val);
      if (data.iv) setIvBase64(data.iv);
      if (data.wrappedKey || data.aesKey || data.key) setWrappedKeyBase64(data.wrappedKey || data.aesKey || data.key);
      if (data.originalName || data.fileName) setOriginalName(data.originalName || data.fileName);
      if (data.iv || data.wrappedKey || data.aesKey) {
        showToast('Metadata parameters auto-filled from JSON!', 'success');
      }
    } catch {
      // not yet valid json, ignore
    }
  };

  const handleDecrypt = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!encryptedFile) {
      setError('Please select an encrypted file to decrypt.');
      return;
    }
    if (!ivBase64.trim()) {
      setError('Initialization Vector (IV) is required. Paste JSON metadata or enter IV manually.');
      return;
    }
    if (!wrappedKeyBase64.trim()) {
      setError('Symmetric Key / Wrapped Key is required.');
      return;
    }

    setDecrypting(true);
    setError(null);
    setDecryptedBlobUrl(null);

    try {
      showToast('Reading encrypted file ciphertext...', 'info');
      const ciphertextArrayBuffer = await encryptedFile.arrayBuffer();

      showToast('Importing AES decryption key...', 'info');
      const keyBuffer = base64ToArrayBuffer(wrappedKeyBase64);

      const aesKey = await globalThis.crypto.subtle.importKey(
        'raw',
        keyBuffer,
        { name: 'AES-GCM', length: 256 },
        false,
        ['decrypt']
      );

      const ivBuffer = base64ToArrayBuffer(ivBase64);

      showToast('Decrypting payload via AES-256-GCM...', 'info');
      const decryptedPlaintextBuffer = await globalThis.crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: ivBuffer,
        },
        aesKey,
        ciphertextArrayBuffer
      );

      const blob = new Blob([decryptedPlaintextBuffer]);
      const url = URL.createObjectURL(blob);
      setDecryptedBlobUrl(url);
      setDecryptedFileName(originalName || encryptedFile.name.replace(/\.enc$/, '') || 'decrypted-document');

      showToast('File decrypted successfully!', 'success');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Decryption failed. Invalid IV, key, or corrupted ciphertext tag.';
      setError(msg);
      showToast('Decryption failed: ' + msg, 'error');
    } finally {
      setDecrypting(false);
    }
  };

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h1
          style={{
            fontSize: '1.75rem',
            fontWeight: 700,
            color: 'var(--text-primary)',
            letterSpacing: '-0.03em',
            marginBottom: '0.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <Unlock size={28} color="var(--accent-primary)" />
          Decryption Workbench
        </h1>
        <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
          Secure offline & client-side zero-knowledge decryption space. Upload any `.enc` package along with its cryptographic IV and AES key to restore original files.
        </p>
      </div>

      {error && (
        <div
          style={{
            background: 'var(--status-danger-bg)',
            border: '1px solid var(--status-danger-border)',
            color: 'var(--status-danger)',
            padding: '1rem 1.25rem',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            fontSize: '0.875rem',
          }}
        >
          <AlertCircle size={20} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        <Card title="Encrypted Payload & Parameters" subtitle="Provide ciphertext and cryptographic keys">
          <form onSubmit={handleDecrypt} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  marginBottom: '0.5rem',
                }}
              >
                Encrypted File (.enc)
              </label>
              <div
                style={{
                  border: '2px dashed var(--border-glass)',
                  borderRadius: 'var(--radius-md)',
                  padding: '1.25rem',
                  textAlign: 'center',
                  background: 'rgba(255, 255, 255, 0.02)',
                  cursor: 'pointer',
                  position: 'relative',
                }}
              >
                <input
                  type="file"
                  onChange={handleFileChange}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: '100%',
                    opacity: 0,
                    cursor: 'pointer',
                  }}
                />
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.4rem' }}>
                  <Upload size={22} color="var(--accent-primary)" />
                  <span style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-primary)' }}>
                    {encryptedFile ? encryptedFile.name : 'Click or drop encrypted file here'}
                  </span>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                    {encryptedFile ? `${(encryptedFile.size / 1024).toFixed(1)} KB` : 'Supports any .enc binary package'}
                  </span>
                </div>
              </div>
            </div>

            {/* JSON Metadata Quick Auto-Fill */}
            <div>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  marginBottom: '0.35rem',
                }}
              >
                <Code size={14} color="var(--accent-primary)" />
                Quick Auto-Fill (Paste JSON Metadata)
              </label>
              <textarea
                value={jsonMetadataInput}
                onChange={(e) => handleJsonMetadataChange(e.target.value)}
                placeholder={'{\n  "iv": "...",\n  "wrappedKey": "...",\n  "originalName": "document.pdf"\n}'}
                rows={3}
                style={{
                  width: '100%',
                  padding: '0.5rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-glass)',
                  color: 'var(--text-primary)',
                  fontFamily: 'monospace',
                  fontSize: '0.75rem',
                  resize: 'vertical',
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  marginBottom: '0.35rem',
                }}
              >
                Initialization Vector (IV - Base64)
              </label>
              <input
                type="text"
                value={ivBase64}
                onChange={(e) => setIvBase64(e.target.value)}
                placeholder="e.g. dGhpcy1pcy1hbi1pdg=="
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-glass)',
                  color: 'var(--text-primary)',
                  fontFamily: 'monospace',
                  fontSize: '0.8125rem',
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  marginBottom: '0.35rem',
                }}
              >
                AES Symmetric Key (Base64)
              </label>
              <input
                type="text"
                value={wrappedKeyBase64}
                onChange={(e) => setWrappedKeyBase64(e.target.value)}
                placeholder="e.g. c3ltbWV0cmljLWtleS1iYXNlNjQ="
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-glass)',
                  color: 'var(--text-primary)',
                  fontFamily: 'monospace',
                  fontSize: '0.8125rem',
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--text-secondary)',
                  marginBottom: '0.35rem',
                }}
              >
                Decrypted Output Filename
              </label>
              <input
                type="text"
                value={originalName}
                onChange={(e) => setOriginalName(e.target.value)}
                placeholder="document.pdf"
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-primary)',
                  border: '1px solid var(--border-glass)',
                  color: 'var(--text-primary)',
                  fontSize: '0.875rem',
                }}
              />
            </div>

            <Button type="submit" variant="primary" icon={<Unlock size={16} />} disabled={decrypting}>
              {decrypting ? 'Decrypting Payload...' : 'Decrypt File Locally'}
            </Button>
          </form>
        </Card>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <Card title="Decryption Status" subtitle="Zero-Knowledge Client Verification">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', padding: '0.5rem 0' }}>
              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.02)',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-glass)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                }}
              >
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    background: decryptedBlobUrl ? 'rgba(16, 185, 129, 0.15)' : 'rgba(99, 102, 241, 0.15)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {decryptedBlobUrl ? (
                    <ShieldCheck size={22} color="#10b981" />
                  ) : (
                    <Key size={22} color="var(--accent-primary)" />
                  )}
                </div>
                <div>
                  <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                    {decryptedBlobUrl ? 'Decryption Successful' : 'Awaiting Ciphertext & Key'}
                  </h4>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {decryptedBlobUrl
                      ? 'Plaintext integrity verified via AES-256-GCM authentication tag.'
                      : 'Provide file, IV, and key to initiate local decryption.'}
                  </p>
                </div>
              </div>

              {decryptedBlobUrl && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div
                    style={{
                      background: 'var(--bg-primary)',
                      padding: '1rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-glass)',
                    }}
                  >
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                      Decrypted File
                    </div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <FileText size={16} color="#10b981" />
                      {decryptedFileName}
                    </div>
                  </div>

                  <a
                    href={decryptedBlobUrl}
                    download={decryptedFileName}
                    style={{ textDecoration: 'none' }}
                  >
                    <Button variant="primary" icon={<FileText size={16} />} style={{ width: '100%', justifyContent: 'center' }}>
                      Download Plaintext File
                    </Button>
                  </a>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
