'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createLicenseServer } = require('../server.js');

async function withServer(run) {
  const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'tutien-license-test-'));
  const app = createLicenseServer({ dataDir, adminToken: 'test-admin-token-change-this-value-987654321' });
  await new Promise((resolve, reject) => {
    app.server.once('error', reject);
    app.server.listen(0, '127.0.0.1', resolve);
  });
  const address = app.server.address();
  const base = `http://127.0.0.1:${address.port}`;
  async function call(route, { method='GET', token, body } = {}) {
    const response = await fetch(base + route, {
      method,
      headers: { ...(token ? { Authorization:`Bearer ${token}` } : {}), ...(body !== undefined ? {'Content-Type':'application/json'} : {}) },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    let parsed = {};
    try { parsed = JSON.parse(text); } catch { /* Dashboard HTML is expected on root. */ }
    return { status: response.status, body: parsed, text };
  }
  try { await run({ call, dataDir, adminToken: app.adminToken }); }
  finally {
    await new Promise(resolve => app.server.close(resolve));
    fs.rmSync(dataDir, { recursive:true, force:true });
  }
}

test('health endpoint responds without admin credentials', async () => {
  await withServer(async ({ call }) => {
    const response = await call('/api/health');
    assert.equal(response.status, 200);
    assert.equal(response.body.ok, true);
  });
});

test('admin routes require a bearer token and persist only key hashes', async () => {
  await withServer(async ({ call, dataDir, adminToken }) => {
    assert.equal((await call('/api/admin/licenses')).status, 401);
    const issued = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', durationDays:14, owner:'Test Server' } });
    assert.equal(issued.status, 201);
    assert.match(issued.body.key, /^TTF-/);
    assert.equal(issued.body.license.durationDays, 0, 'FREE is perpetual');
    assert.equal(issued.body.license.expiresAt, null, 'FREE has no expiry timestamp');
    const persisted = fs.readFileSync(path.join(dataDir, 'licenses.json'), 'utf8');
    assert.equal(persisted.includes(issued.body.key), false);
    assert.equal(persisted.includes('keyHash'), true);
  });
});

test('free license activates once per public IP and locks installation identity', async () => {
  await withServer(async ({ call, adminToken, dataDir }) => {
    const first = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', owner:'First host' } });
    const second = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', owner:'Second host' } });
    const payload = { licenseKey:first.body.key, instanceId:'11111111-1111-4111-8111-111111111111', edition:'FREE', serverName:'Paper #1' };
    assert.equal((await call('/api/activate',{method:'POST',body:payload})).status,200);
    assert.equal((await call('/api/validate',{method:'POST',body:payload})).status,200);
    const otherInstall = { ...payload, instanceId:'22222222-2222-4222-8222-222222222222' };
    assert.equal((await call('/api/validate',{method:'POST',body:otherInstall})).status,409);
    // A perpetual Free key stays usable while this IP cannot farm another Free key.
    const dbPath = path.join(dataDir, 'licenses.json');
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf8'));
    const firstRecord = db.licenses.find(item => item.keyHash);
    firstRecord.status = 'expired';
    firstRecord.expiresAt = Date.now() - 1000;
    fs.writeFileSync(dbPath, JSON.stringify(db, null, 2));
    const otherFree = { ...payload, licenseKey:second.body.key, instanceId:'33333333-3333-4333-8333-333333333333' };
    const blocked = await call('/api/activate',{method:'POST',body:otherFree});
    assert.equal(blocked.status,409);
    assert.equal(blocked.body.status,'free_ip_limit');
  });
});


test('legacy FREE keys migrate from 365-day expiry to perpetual access', async () => {
  await withServer(async ({ call, adminToken, dataDir }) => {
    const created = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', owner:'Legacy Free host' } });
    const payload = { licenseKey:created.body.key, instanceId:'77777777-7777-4777-8777-777777777777', edition:'FREE', serverName:'Legacy host' };
    assert.equal((await call('/api/activate',{method:'POST',body:payload})).status, 200);

    // Simulate a persisted record created by the previous 365-day policy.
    const dbPath = path.join(dataDir, 'licenses.json');
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf8'));
    const legacy = db.licenses.find(item => item.id === created.body.license.id);
    legacy.durationDays = 365;
    legacy.expiresAt = Date.now() - 1000;
    legacy.status = 'expired';
    fs.writeFileSync(dbPath, JSON.stringify(db, null, 2));

    const validated = await call('/api/validate', { method:'POST', body:payload });
    assert.equal(validated.status, 200, 'old Free keys are renewed as perpetual by policy migration');
    assert.equal(validated.body.expiresAt, null);
    const migrated = JSON.parse(fs.readFileSync(dbPath, 'utf8')).licenses.find(item => item.id === created.body.license.id);
    assert.equal(migrated.durationDays, 0);
    assert.equal(migrated.expiresAt, null);
    assert.equal(migrated.status, 'active');
  });
});

test('FREE key cannot activate a PREMIUM build; premium keys bind after activate', async () => {
  await withServer(async ({ call, adminToken }) => {
    const free = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', owner:'Trial' } });
    const wrongPlan = await call('/api/activate',{method:'POST',body:{licenseKey:free.body.key,instanceId:'11111111-1111-4111-8111-111111111111',edition:'PREMIUM'}});
    assert.equal(wrongPlan.status,403);
    assert.equal(wrongPlan.body.status,'wrong_plan');
    const premium = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'PREMIUM', durationDays:365, owner:'Paid host' } });
    const valid = await call('/api/activate',{method:'POST',body:{licenseKey:premium.body.key,instanceId:'44444444-4444-4444-8444-444444444444',edition:'PREMIUM',serverName:'Premium Server'}});
    assert.equal(valid.status,200);
    assert.equal(valid.body.plan,'PREMIUM');
    assert.ok(valid.body.expiresAt);
  });
});

test('admin can unbind after a valid migration and revoke permanently', async () => {
  await withServer(async ({ call, adminToken }) => {
    const issued = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'PREMIUM', durationDays:30, owner:'Migration' } });
    const id = issued.body.license.id;
    const payload = { licenseKey:issued.body.key, instanceId:'55555555-5555-4555-8555-555555555555', edition:'PREMIUM' };
    const firstActivation = await call('/api/activate',{method:'POST',body:payload});
    assert.equal(firstActivation.status,200);
    const expiryBeforeUnbind = firstActivation.body.expiresAt;
    const unbound = await call(`/api/admin/licenses/${id}/unbind`,{method:'POST',token:adminToken,body:{}});
    assert.equal(unbound.status,200);
    assert.equal(unbound.body.license.expiresAt, expiryBeforeUnbind, 'unbind must preserve the original expiration');
    const reactivated = await call('/api/activate',{method:'POST',body:{...payload,instanceId:'66666666-6666-4666-8666-666666666666'}});
    assert.equal(reactivated.status,200);
    assert.equal(reactivated.body.expiresAt, expiryBeforeUnbind, 'host migration must not reset a paid term');
    assert.equal((await call(`/api/admin/licenses/${id}/revoke`,{method:'POST',token:adminToken,body:{}})).status,200);
    const revoked = await call('/api/validate',{method:'POST',body:{...payload,instanceId:'66666666-6666-4666-8666-666666666666'}});
    assert.equal(revoked.status,403);
    assert.equal(revoked.body.status,'revoked');
  });
});


test('dashboard is served and oversized JSON is rejected cleanly', async () => {
  await withServer(async ({ call, adminToken }) => {
    // Dashboard markup is served by the same-origin API; Tailwind is intentionally loaded by CDN.
    const dashboard = await call('/');
    assert.equal(dashboard.status, 200);
    assert.match(dashboard.text || '', /Tu Tiên · License Control/);
    const tooLarge = await call('/api/admin/licenses', { method:'POST', token:adminToken, body:{ plan:'FREE', note:'x'.repeat(30_000) } });
    assert.equal(tooLarge.status, 413);
  });
});
