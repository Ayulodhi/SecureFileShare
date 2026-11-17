import test from 'node:test';
import assert from 'node:assert';
import { saveUserKeys, getUserKeys, deleteUserKeys, StoredUserKeys } from './keyVault.ts';

test('TDD Browser Vault Namespacing & Isolation: User B cannot read User A keys from vault', async () => {
    const aliceKeys: StoredUserKeys = {
        username: 'alice',
        rsaPrivateKey: {} as CryptoKey,
        ecPrivateKey: {} as CryptoKey,
        rsaPublicKeyPem: '-----BEGIN PUBLIC KEY----- alice -----END PUBLIC KEY-----',
        ecPublicKeyPem: '-----BEGIN PUBLIC KEY----- alice ec -----END PUBLIC KEY-----',
        fingerprint: 'aaaabbbbccccdddd',
        createdAt: new Date().toISOString()
    };

    // Alice saves her keys
    await saveUserKeys(aliceKeys);

    // Bob attempts to retrieve keys using his own username
    const bobKeys = await getUserKeys('bob');
    assert.strictEqual(bobKeys, null, 'User B must not be able to read User A keys');

    // Alice retrieves her own keys successfully
    const retrievedAlice = await getUserKeys('alice');
    assert.notStrictEqual(retrievedAlice, null);
    assert.strictEqual(retrievedAlice?.username, 'alice');

    // Logout / cleanup deletes keys
    await deleteUserKeys('alice');
    const afterLogout = await getUserKeys('alice');
    assert.strictEqual(afterLogout, null, 'Keys must be wiped/cleared on logout');
});
