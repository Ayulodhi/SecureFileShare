import { useState } from 'react';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Users, UserPlus, Copy, Check, ShieldAlert } from 'lucide-react';

export function FriendsPage() {
  const [friendCode, setFriendCode] = useState('');
  const [copied, setCopied] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSendRequest = (e: React.FormEvent) => {
    e.preventDefault();
    if (!friendCode.trim()) return;
    setSuccessMsg(`Friend request sent to ${friendCode.trim()}!`);
    setErrorMsg(null);
    setFriendCode('');
  };

  const handleCopyInvite = () => {
    navigator.clipboard.writeText('https://securefileshare.com/invite/SFS-INV-DEMO123456');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      <div>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem', letterSpacing: '-0.025em' }}>
          Friends & Secure Sharing
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Connect with trusted friends, send direct sharing keys, and manage invite links.
        </p>
      </div>

      {successMsg && (
        <div style={{ padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-md)', color: '#34d399', fontSize: '0.9rem' }}>
          {successMsg}
        </div>
      )}

      {errorMsg && (
        <div style={{ padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <ShieldAlert size={18} /> {errorMsg}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Add Friend Card */}
        <Card title="Add Friend" subtitle="Connect using a friend handle or code">
          <form onSubmit={handleSendRequest} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                Friend Code / Handle
              </label>
              <input
                type="text"
                value={friendCode}
                onChange={(e) => setFriendCode(e.target.value)}
                placeholder="e.g. SFS-7X9K"
                style={{
                  width: '100%',
                  padding: '0.75rem 1rem',
                  background: 'var(--bg-tertiary)',
                  border: '1px solid var(--border-glass)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--text-primary)',
                  fontSize: '0.95rem',
                  outline: 'none',
                }}
              />
            </div>
            <Button type="submit" variant="primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
              <UserPlus size={18} /> Send Friend Request
            </Button>
          </form>
        </Card>

        {/* Generate Invite Link Card */}
        <Card title="Invite Link" subtitle="Share a secure invite link with anyone">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                Your Secure Invite URL (7-day expiry, single-use)
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type="text"
                  readOnly
                  value="https://securefileshare.com/invite/SFS-INV-DEMO123456"
                  style={{
                    flex: 1,
                    padding: '0.75rem 1rem',
                    background: 'var(--bg-tertiary)',
                    border: '1px solid var(--border-glass)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--text-secondary)',
                    fontSize: '0.85rem',
                    outline: 'none',
                  }}
                />
                <Button type="button" variant="secondary" onClick={handleCopyInvite} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  {copied ? <Check size={16} color="#34d399" /> : <Copy size={16} />}
                  {copied ? 'Copied' : 'Copy'}
                </Button>
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Friends List Section */}
      <Card title="Your Friends" subtitle="Connected friends eligible for direct zero-knowledge sharing">
        <div style={{ padding: '2rem 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Users size={36} style={{ marginBottom: '0.75rem', opacity: 0.5 }} />
          <p style={{ fontSize: '0.95rem' }}>No friends connected yet. Send a friend request above to get started!</p>
        </div>
      </Card>
    </div>
  );
}
