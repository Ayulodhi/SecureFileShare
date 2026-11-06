import { useState } from 'react';
import { Modal } from './Modal';
import { Button } from './Button';
import { shareApi, ShareRecord } from '../api/shareApi';
import { FileMetadataResponse } from '../api/fileApi';
import { showToast } from './Toast';
import { Share2, Copy, Check, Calendar, User } from 'lucide-react';

interface ShareModalProps {
  file?: FileMetadataResponse;
  fileId?: string;
  fileName?: string;
  isOpen?: boolean;
  onClose: () => void;
  onShareCreated?: (share: ShareRecord) => void;
}

export function ShareModal({
  file,
  fileId: propFileId,
  fileName: propFileName,
  isOpen = true,
  onClose,
  onShareCreated,
}: ShareModalProps) {
  const targetFileId = file?.id || propFileId || '';
  const targetFileName = file?.originalName || propFileName || 'File';

  const [recipient, setRecipient] = useState('');
  const [expiry, setExpiry] = useState('24 hours');
  const [loading, setLoading] = useState(false);
  const [createdShare, setCreatedShare] = useState<ShareRecord | null>(null);
  const [copied, setCopied] = useState(false);

  const handleCreate = async () => {
    setLoading(true);
    try {
      const share = await shareApi.createShareLink({
        fileId: targetFileId,
        fileName: targetFileName,
        recipientUsername: recipient.trim() || 'public',
        expiryOption: expiry,
      });

      setCreatedShare(share);
      if (onShareCreated) onShareCreated(share);
      showToast('Share link generated.', 'success');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to generate share link';
      showToast(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (createdShare) {
      navigator.clipboard.writeText(createdShare.shareLink);
      setCopied(true);
      showToast('Share link copied to clipboard.', 'info');
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handleReset = () => {
    setCreatedShare(null);
    setRecipient('');
    setExpiry('24 hours');
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleReset} title="Generate Share Authorization" maxWidth="480px">
      {!createdShare ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <span
              style={{
                fontSize: '0.68rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
              }}
            >
              Target Resource
            </span>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginTop: '0.2rem' }}>
              {targetFileName}
            </div>
          </div>

          <div>
            <label
              style={{
                display: 'block',
                fontSize: '0.72rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                marginBottom: '0.4rem',
                color: 'var(--text-muted)',
              }}
            >
              Recipient Username (Optional)
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Leave blank for any recipient with link"
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
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

          <div>
            <label
              style={{
                display: 'block',
                fontSize: '0.72rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                marginBottom: '0.4rem',
                color: 'var(--text-muted)',
              }}
            >
              Authorization Expiry
            </label>
            <div style={{ position: 'relative' }}>
              <select
                className="form-input"
                value={expiry}
                onChange={(e) => setExpiry(e.target.value)}
                style={{ paddingLeft: '2.4rem' }}
              >
                <option value="1 hour">1 hour</option>
                <option value="24 hours">24 hours</option>
                <option value="7 days">7 days</option>
                <option value="30 days">30 days</option>
              </select>
              <Calendar
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
            <Button variant="secondary" onClick={handleReset}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleCreate}
              loading={loading}
              icon={<Share2 size={13} />}
            >
              Generate Link
            </Button>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div
            style={{
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-sharp)',
              background: 'var(--status-clean-bg)',
              border: '1px solid var(--status-clean-border)',
              color: 'var(--status-clean)',
              fontSize: '0.8125rem',
              fontWeight: 600,
            }}
          >
            AUTHORIZATION TOKEN GENERATED
          </div>

          <div>
            <label
              style={{
                display: 'block',
                fontSize: '0.72rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                marginBottom: '0.4rem',
                color: 'var(--text-muted)',
              }}
            >
              Share URL
            </label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                type="text"
                readOnly
                className="form-input font-mono"
                value={createdShare.shareLink}
                style={{ fontSize: '0.8rem', background: 'var(--bg-input)' }}
              />
              <Button
                variant="primary"
                onClick={handleCopy}
                icon={copied ? <Check size={13} /> : <Copy size={13} />}
              >
                {copied ? 'Copied' : 'Copy'}
              </Button>
            </div>
          </div>

          <div className="font-mono" style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            EXPIRATION: {new Date(createdShare.expiresAt).toISOString().replace('T', ' ').substring(0, 19)}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <Button variant="secondary" onClick={handleReset}>
              Close
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
}
