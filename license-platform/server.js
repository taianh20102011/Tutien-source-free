'use strict';

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { URL } = require('node:url');

const PORT = Number.parseInt(process.env.PORT || '8787', 10);
const HOST = process.env.HOST || '127.0.0.1';
const DEFAULT_DATA_DIR = path.join(__dirname, 'data');
const PAGE = path.join(__dirname, 'public', 'index.html');
const MAX_BODY_BYTES = 24 * 1024;
const RATE_WINDOW_MS = 5 * 60 * 1000;
const RATE_LIMIT = 100;

function normalizeIp(raw) {
  let ip = String(raw || '').trim();
  if (ip.startsWith('::ffff:')) ip = ip.slice(7);
  if (ip === '::1') return '127.0.0.1';
  // Do not silently trust X-Forwarded-For; it is enabled only for a configured reverse proxy.
  return ip || 'unknown';
}

function observedClientIp(req) {
  if (process.env.TRUST_PROXY === 'true') {
    const forwarded = req.headers['x-forwarded-for'];
    if (typeof forwarded === 'string' && forwarded.trim()) {
      return normalizeIp(forwarded.split(',')[0]);
    }
  }
  return normalizeIp(req.socket.remoteAddress);
}

function keyHash(key) {
  return crypto.createHash('sha256').update(String(key || '').trim().toUpperCase()).digest('hex');
}

function createLicenseKey(plan) {
  const prefix = plan === 'PREMIUM' ? 'TTP' : 'TTF';
  const raw = crypto.randomBytes(16).toString('hex').toUpperCase();
  return `${prefix}-${raw.match(/.{1,4}/g).join('-')}`;
}

function safeString(value, maxLength = 120) {
  return typeof value === 'string' ? value.trim().slice(0, maxLength) : '';
}

function isExpired(license, now = Date.now()) {
  // FREE is a perpetual edition. Ignore historical expiration timestamps so
  // upgrading the license service does not unexpectedly disable existing Free hosts.
  if (String(license && license.plan || '').toUpperCase() === 'FREE') return false;
  return Number.isFinite(license.expiresAt) && license.expiresAt > 0 && now >= license.expiresAt;
}

function toIso(ms) {
  return Number.isFinite(ms) && ms > 0 ? new Date(ms).toISOString() : null;
}

function publicLicense(license) {
  return {
    id: license.id,
    plan: license.plan,
    status: isExpired(license) && license.status !== 'revoked' ? 'expired' : license.status,
    owner: license.owner,
    note: license.note,
    createdAt: toIso(license.createdAt),
    activatedAt: toIso(license.activatedAt),
    expiresAt: toIso(license.expiresAt),
    durationDays: license.durationDays,
    boundIp: license.boundIp || null,
    instanceId: license.instanceId || null,
    serverName: license.serverName || null,
    lastValidatedAt: toIso(license.lastValidatedAt),
    keyHint: license.keyHint,
  };
}

function sendJson(res, statusCode, value) {
  const body = JSON.stringify(value);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'no-referrer',
  });
  res.end(body);
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    let tooLarge = false;
    let settled = false;
    req.on('data', chunk => {
      if (tooLarge) return; // Drain without buffering more data; still allow a 413 response.
      data += chunk;
      if (Buffer.byteLength(data) > MAX_BODY_BYTES) {
        tooLarge = true;
        data = '';
      }
    });
    req.on('end', () => {
      if (settled) return;
      settled = true;
      if (tooLarge) return reject(Object.assign(new Error('Request quá lớn.'), { statusCode: 413 }));
      if (!data) return resolve({});
      try {
        const value = JSON.parse(data);
        if (!value || typeof value !== 'object' || Array.isArray(value)) {
          return reject(Object.assign(new Error('JSON body phải là object.'), { statusCode: 400 }));
        }
        resolve(value);
      } catch {
        reject(Object.assign(new Error('JSON không hợp lệ.'), { statusCode: 400 }));
      }
    });
    req.on('error', error => {
      if (settled) return;
      settled = true;
      reject(error);
    });
  });
}

function secureEqual(a, b) {
  const left = Buffer.from(String(a || ''), 'utf8');
  const right = Buffer.from(String(b || ''), 'utf8');
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function getAdminToken(dataDir) {
  const fromEnv = safeString(process.env.TUTIEN_ADMIN_TOKEN, 300);
  if (fromEnv.length >= 24) return fromEnv;
  const tokenFile = path.join(dataDir, 'admin-token.txt');
  if (fs.existsSync(tokenFile)) {
    const stored = fs.readFileSync(tokenFile, 'utf8').trim();
    if (stored.length >= 24) return stored;
  }

  const token = crypto.randomBytes(32).toString('base64url');
  fs.mkdirSync(dataDir, { recursive: true, mode: 0o700 });
  if (fs.existsSync(tokenFile)) {
    // Repair an invalid/short token file left by a bad manual configuration.
    fs.writeFileSync(tokenFile, token + '\n', { mode: 0o600 });
  } else {
    try {
      fs.writeFileSync(tokenFile, token + '\n', { mode: 0o600, flag: 'wx' });
    } catch (error) {
      // In case another process initialized the token at the same time, reuse it.
      if (error.code !== 'EEXIST') throw error;
      const raced = fs.readFileSync(tokenFile, 'utf8').trim();
      if (raced.length >= 24) return raced;
      fs.writeFileSync(tokenFile, token + '\n', { mode: 0o600 });
    }
  }
  console.log('\n[TuTien License] Đã tạo admin token mới. Lưu token này để đăng nhập dashboard:');
  console.log(token);
  console.log(`Token file: ${tokenFile}\n`);
  return token;
}

function loadDatabase(filePath) {
  let parsed;
  try {
    parsed = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch { /* A first-run or missing database is normal. */ }
  if (!parsed || !Array.isArray(parsed.licenses)) return { version: 1, licenses: [] };

  // Migrate every older FREE record to perpetual use. This also restores
  // licenses previously marked expired only because the old policy was 365 days.
  // Revoked licenses remain revoked; IP/installation bindings are preserved.
  let migrated = false;
  for (const license of parsed.licenses) {
    if (String(license && license.plan || '').toUpperCase() !== 'FREE') continue;
    if (license.durationDays !== 0) { license.durationDays = 0; migrated = true; }
    if (license.expiresAt !== null) { license.expiresAt = null; migrated = true; }
    if (license.status === 'expired') {
      license.status = license.boundIp && license.instanceId ? 'active' : 'new';
      migrated = true;
    }
  }
  // Let disk-write errors surface instead of disguising the existing database as empty.
  if (migrated) saveDatabase(filePath, parsed);
  return parsed;
}

function saveDatabase(filePath, db) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true, mode: 0o700 });
  const temp = `${filePath}.tmp`;
  fs.writeFileSync(temp, JSON.stringify(db, null, 2) + '\n', { mode: 0o600 });
  fs.renameSync(temp, filePath);
}

function createLicenseServer(options = {}) {
  const dataDir = options.dataDir || process.env.TUTIEN_DATA_DIR || DEFAULT_DATA_DIR;
  fs.mkdirSync(dataDir, { recursive: true, mode: 0o700 });
  const providedAdminToken = safeString(options.adminToken, 300);
  const adminToken = providedAdminToken.length >= 24 ? providedAdminToken : getAdminToken(dataDir);
  const dbFile = path.join(dataDir, 'licenses.json');
  const rateMap = new Map();
  let requestSequence = 0;

  function readDb() { return loadDatabase(dbFile); }
  function writeDb(db) { saveDatabase(dbFile, db); }

  function limited(ip) {
    const now = Date.now();
    const previous = rateMap.get(ip) || [];
    const recent = previous.filter(ts => now - ts < RATE_WINDOW_MS);
    recent.push(now);
    rateMap.set(ip, recent);
    if (requestSequence++ % 100 === 0 || rateMap.size > 10000) {
      for (const [key, values] of rateMap) {
        if (values.every(ts => now - ts >= RATE_WINDOW_MS)) rateMap.delete(key);
      }
      // Bound memory if a flood rotates source IPs faster than expiry cleanup.
      while (rateMap.size > 12000) rateMap.delete(rateMap.keys().next().value);
    }
    return recent.length > RATE_LIMIT;
  }

  function authorized(req) {
    const header = req.headers.authorization || '';
    const match = /^Bearer\s+(.+)$/i.exec(header);
    return !!match && secureEqual(match[1], adminToken);
  }

  function activateOrValidate(body, ip, allowBinding) {
    const licenseKey = safeString(body.licenseKey, 200).toUpperCase();
    const instanceId = safeString(body.instanceId, 100);
    const edition = safeString(body.edition, 20).toUpperCase();
    const serverName = safeString(body.serverName, 80);
    if (!licenseKey || !instanceId || !/^[A-F0-9-]{16,100}$/i.test(instanceId)) {
      return { code: 400, result: { ok: false, status: 'bad_request', message: 'Thiếu licenseKey hoặc instanceId không hợp lệ.' } };
    }
    if (!['FREE', 'PREMIUM'].includes(edition)) {
      return { code: 400, result: { ok: false, status: 'bad_edition', message: 'Edition chỉ chấp nhận FREE hoặc PREMIUM.' } };
    }

    const db = readDb();
    const digest = keyHash(licenseKey);
    const license = db.licenses.find(item => item.keyHash === digest);
    if (!license) return { code: 404, result: { ok: false, status: 'not_found', message: 'License key không tồn tại.' } };
    if (license.status === 'revoked') return { code: 403, result: { ok: false, status: 'revoked', message: 'License đã bị thu hồi.' } };
    if (isExpired(license)) {
      license.status = 'expired';
      writeDb(db);
      return { code: 403, result: { ok: false, status: 'expired', message: 'License đã hết hạn.' } };
    }
    if (edition === 'PREMIUM' && license.plan !== 'PREMIUM') {
      return { code: 403, result: { ok: false, status: 'wrong_plan', message: 'Bản Premium yêu cầu license Premium.' } };
    }

    if (license.boundIp && license.boundIp !== ip) {
      return { code: 409, result: { ok: false, status: 'host_ip_mismatch', message: 'License đã khóa vào IP host khác. Hãy yêu cầu admin reset binding nếu đã đổi IP.' } };
    }
    if (license.instanceId && license.instanceId !== instanceId) {
      return { code: 409, result: { ok: false, status: 'instance_mismatch', message: 'License đang gắn với một installation khác.' } };
    }
    if ((!license.boundIp || !license.instanceId) && !allowBinding) {
      return { code: 409, result: { ok: false, status: 'not_activated', message: 'License chưa được activate.' } };
    }

    if (allowBinding && (!license.boundIp || !license.instanceId)) {
      // Anti-abuse rule: a public host IP can ever bind one FREE license unless an admin explicitly unbinds it.
      // Premium keys are purchase-bound; repeated issuance does not create extra free trials.
      if (license.plan === 'FREE') {
        const anotherFree = db.licenses.find(item => item.id !== license.id
          && item.plan === 'FREE' && item.boundIp === ip);
        if (anotherFree) {
          return { code: 409, result: { ok: false, status: 'free_ip_limit', message: 'IP host này đã dùng một license FREE. Dùng license hiện tại hoặc yêu cầu admin reset binding.' } };
        }
      }
      license.boundIp = ip;
      license.instanceId = instanceId;
      // Start the subscription clock only once. An admin unbind for a legitimate
      // Host migration must never reset a license term. FREE has no expiry; Premium preserves its original term.
      if (!license.activatedAt) {
        license.activatedAt = Date.now();
        if (license.durationDays > 0) license.expiresAt = license.activatedAt + license.durationDays * 24 * 60 * 60 * 1000;
      }
      license.status = 'active';
    }

    license.status = 'active';
    license.serverName = serverName || license.serverName || '';
    license.lastValidatedAt = Date.now();
    license.lastSeenIp = ip;
    writeDb(db);
    return {
      code: 200,
      result: {
        ok: true,
        status: 'active',
        plan: license.plan,
        edition,
        expiresAt: toIso(license.expiresAt),
        message: 'License hợp lệ và host binding đã được xác minh.',
      },
    };
  }

  const server = http.createServer(async (req, res) => {
    const ip = observedClientIp(req);
    const url = new URL(req.url || '/', 'http://localhost');
    const pathname = url.pathname;

    if (limited(ip)) return sendJson(res, 429, { ok: false, status: 'rate_limited', message: 'Quá nhiều yêu cầu. Thử lại sau vài phút.' });

    if (req.method === 'GET' && pathname === '/api/health') {
      return sendJson(res, 200, { ok: true, service: 'TuTien License Control', version: 1, timestamp: new Date().toISOString() });
    }

    if (req.method === 'GET' && pathname === '/') {
      try {
        const html = fs.readFileSync(PAGE);
        res.writeHead(200, {
          'Content-Type': 'text/html; charset=utf-8',
          'Content-Length': html.length,
          'Cache-Control': 'no-cache',
          'X-Content-Type-Options': 'nosniff',
          'X-Frame-Options': 'DENY',
          'Referrer-Policy': 'no-referrer',
          'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
        });
        return res.end(html);
      } catch {
        return sendJson(res, 500, { ok: false, message: 'Không đọc được giao diện dashboard.' });
      }
    }

    if (pathname.startsWith('/api/admin/')) {
      if (!authorized(req)) return sendJson(res, 401, { ok: false, status: 'unauthorized', message: 'Admin token không hợp lệ.' });
      try {
        if (req.method === 'GET' && pathname === '/api/admin/licenses') {
          const db = readDb();
          return sendJson(res, 200, { ok: true, licenses: db.licenses.map(publicLicense).sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')) });
        }

        if (req.method === 'POST' && pathname === '/api/admin/licenses') {
          const body = await readJson(req);
          const plan = safeString(body.plan, 20).toUpperCase();
          if (!['FREE', 'PREMIUM'].includes(plan)) return sendJson(res, 400, { ok: false, message: 'Plan phải là FREE hoặc PREMIUM.' });
          let durationDays = Number.parseInt(body.durationDays, 10);
          if (plan === 'FREE') durationDays = 0; // Perpetual; 0 means no expiry for FREE and PREMIUM lifetime.
          if (plan === 'PREMIUM' && ![0, 30, 90, 180, 365, 730].includes(durationDays)) {
            return sendJson(res, 400, { ok: false, message: 'Premium hỗ trợ thời hạn 30/90/180/365/730 ngày hoặc 0 = lifetime.' });
          }
          const key = createLicenseKey(plan);
          const now = Date.now();
          const license = {
            id: crypto.randomUUID(), keyHash: keyHash(key), keyHint: key.slice(-9), plan,
            status: 'new', owner: safeString(body.owner, 100), note: safeString(body.note, 240),
            createdAt: now, activatedAt: null, expiresAt: null, durationDays,
            boundIp: null, instanceId: null, serverName: '', lastValidatedAt: null,
            lastSeenIp: null, revokedAt: null,
          };
          const db = readDb();
          db.licenses.push(license);
          writeDb(db);
          // The full key is returned once. Only SHA-256 hash + short hint are persisted.
          return sendJson(res, 201, { ok: true, key, license: publicLicense(license) });
        }

        const match = /^\/api\/admin\/licenses\/([0-9a-f-]{36})\/(revoke|unbind)$/.exec(pathname);
        if (req.method === 'POST' && match) {
          const [, id, action] = match;
          const db = readDb();
          const license = db.licenses.find(item => item.id === id);
          if (!license) return sendJson(res, 404, { ok: false, message: 'Không tìm thấy license.' });
          if (action === 'revoke') {
            license.status = 'revoked';
            license.revokedAt = Date.now();
          } else {
            if (license.status === 'revoked') return sendJson(res, 409, { ok: false, message: 'License đã thu hồi; không thể unbind.' });
            license.status = 'new';
            license.boundIp = null;
            license.instanceId = null;
            license.serverName = '';
            // Keep activatedAt/expiresAt so unbinding cannot grant a fresh term.
            license.lastValidatedAt = null;
            license.lastSeenIp = null;
          }
          writeDb(db);
          return sendJson(res, 200, { ok: true, license: publicLicense(license) });
        }
      } catch (error) {
        const code = error.statusCode || 500;
        if (code >= 500) console.error('[TuTien License] Admin API error:', error.message);
        return sendJson(res, code, { ok: false, message: code >= 500 ? 'Lỗi nội bộ.' : error.message });
      }
      return sendJson(res, 404, { ok: false, message: 'Admin endpoint không tồn tại.' });
    }

    if (req.method === 'POST' && (pathname === '/api/activate' || pathname === '/api/validate')) {
      try {
        const body = await readJson(req);
        const result = activateOrValidate(body, ip, pathname === '/api/activate');
        return sendJson(res, result.code, result.result);
      } catch (error) {
        const code = error.statusCode || 500;
        return sendJson(res, code, { ok: false, message: code >= 500 ? 'Lỗi nội bộ.' : error.message });
      }
    }

    return sendJson(res, 404, { ok: false, message: 'Endpoint không tồn tại.' });
  });

  return { server, dataDir, adminToken };
}

function startServer(options = {}) {
  const created = createLicenseServer(options);
  const host = options.host || HOST;
  const port = options.port ?? PORT;
  created.server.listen(port, host, () => {
    const address = created.server.address();
    console.log(`[TuTien License] Dashboard: http://${host}:${address.port}`);
    console.log('[TuTien License] Dùng HTTPS reverse proxy khi đưa lên Internet. Không mở trực tiếp cổng dashboard ra Internet nếu chưa cấu hình TLS/firewall.');
  });
  return created;
}

if (require.main === module) startServer();

module.exports = { createLicenseServer, startServer, keyHash, createLicenseKey, normalizeIp, publicLicense };
