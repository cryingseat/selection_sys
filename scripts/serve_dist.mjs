// 无第三方依赖的前端预览服务器：托管 frontend/dist，并把 /api 代理到 8080。
// 用法：node scripts/serve_dist.mjs [port] [apiTarget]
import http from 'node:http'
import { readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const dist = path.resolve(__dirname, '../frontend/dist')
const port = Number(process.argv[2] || 4173)
const apiTarget = process.argv[3] || 'http://127.0.0.1:8080'
const target = new URL(apiTarget)

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2'
}

http.createServer(async (req, res) => {
  if (req.url.startsWith('/api')) {
    const proxy = http.request({
      hostname: target.hostname,
      port: target.port,
      path: req.url,
      method: req.method,
      headers: req.headers
    }, (upstream) => {
      res.writeHead(upstream.statusCode || 502, upstream.headers)
      upstream.pipe(res)
    })
    proxy.on('error', () => {
      res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
      res.end(JSON.stringify({ code: 50201, message: '后端服务未启动' }))
    })
    req.pipe(proxy)
    return
  }

  try {
    const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname)
    let file = path.join(dist, pathname)
    if (file.endsWith(path.sep) || pathname === '/') file = path.join(dist, 'index.html')
    const info = await stat(file).catch(() => null)
    if (!info || !info.isFile()) file = path.join(dist, 'index.html')
    const body = await readFile(file)
    res.writeHead(200, { 'Content-Type': mime[path.extname(file)] || 'application/octet-stream' })
    res.end(body)
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('preview server error: ' + err.message)
  }
}).listen(port, '127.0.0.1', () => {
  console.log(`Preview: http://127.0.0.1:${port}  (api -> ${apiTarget})`)
})
