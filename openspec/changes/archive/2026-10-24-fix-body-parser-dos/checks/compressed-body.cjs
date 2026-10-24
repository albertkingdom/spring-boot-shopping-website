// Run from a frontend installation: node /path/to/compressed-body.cjs
const assert = require('node:assert/strict');
const http = require('node:http');
const zlib = require('node:zlib');
const { createRequire } = require('node:module');
const requireFrontend = createRequire(`${process.cwd()}/package.json`);
const express = requireFrontend('express');
const bodyParser = requireFrontend('body-parser');
const app = express();
app.use(bodyParser.json());
app.post('/', (req, res) => res.json(req.body));
app.use((error, req, res, next) => res.status(error.status || 500).end());
const server = app.listen(0, '127.0.0.1', async () => {
  const json = JSON.stringify({ probe: 'body-parser' });
  const gzip = zlib.gzipSync(json);
  const send = (payload) => new Promise((resolve, reject) => {
    const req = http.request({ hostname: '127.0.0.1', port: server.address().port,
      method: 'POST', path: '/', headers: { 'Content-Type': 'application/json',
        'Content-Encoding': 'gzip', 'Transfer-Encoding': 'chunked' } }, (res) => {
      let body = '';
      res.on('data', chunk => { body += chunk; });
      res.on('end', () => resolve({ status: res.statusCode, body }));
    });
    req.setTimeout(3000, () => req.destroy(new Error('request timeout')));
    req.on('error', reject);
    req.end(payload);
  });
  try {
    const normal = await send(gzip);
    assert.equal(normal.status, 200);
    assert.deepEqual(JSON.parse(normal.body), JSON.parse(json));
    const malformed = await send(Buffer.concat([gzip, Buffer.from('invalid trailing bytes')]));
    assert.ok([200, 400].includes(malformed.status));
    await new Promise(resolve => setTimeout(resolve, 150));
    const after = await send(gzip);
    assert.equal(after.status, 200);
    console.log(JSON.stringify({ normal: normal.status, malformed: malformed.status,
      after: after.status, version: requireFrontend('body-parser/package.json').version }));
  } catch (error) {
    console.error(error);
    process.exitCode = 1;
  } finally { server.close(); }
});
