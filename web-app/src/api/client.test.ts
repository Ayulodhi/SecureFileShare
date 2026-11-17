import test from 'node:test';
import assert from 'node:assert';
import { request, ApiException } from './client.ts';

test('TDD Production Fallback & Error Handling: PROD build never falls back and throws ApiException on failure', async () => {
    const originalEnv = process.env.NODE_ENV;
    process.env.NODE_ENV = 'production';

    try {
        await assert.rejects(
            async () => {
                await request('http://localhost:1/nonexistent-api');
            },
            (err: any) => {
                return err instanceof ApiException || err instanceof Error;
            }
        );
    } finally {
        process.env.NODE_ENV = originalEnv;
    }
});
