// 把 User-Agent 轉成好讀的「瀏覽器 · 系統」,例如「Chrome · Windows」;判斷順序很重要(Edge 的 UA 也含 Chrome)
const BROWSERS = [
  ['Edg/', 'Edge'],
  ['OPR/', 'Opera'],
  ['SamsungBrowser', 'Samsung 瀏覽器'],
  ['Line/', 'LINE'],
  ['Chrome/', 'Chrome'],
  ['CriOS', 'Chrome'],
  ['Firefox/', 'Firefox'],
  ['FxiOS', 'Firefox'],
  ['Safari/', 'Safari'],
]

const SYSTEMS = [
  ['iPhone', 'iPhone'],
  ['iPad', 'iPad'],
  ['Android', 'Android'],
  ['Windows', 'Windows'],
  ['Mac OS X', 'macOS'],
  ['Macintosh', 'macOS'],
  ['CrOS', 'ChromeOS'],
  ['Linux', 'Linux'],
]

function match(ua, table) {
  const hit = table.find(([needle]) => ua.includes(needle))
  return hit ? hit[1] : null
}

export function describeUserAgent(ua) {
  if (!ua) return '未知裝置'
  const browser = match(ua, BROWSERS)
  const system = match(ua, SYSTEMS)
  if (!browser && !system) return '其他裝置'
  return [browser, system].filter(Boolean).join(' · ')
}

export function isMobileUserAgent(ua) {
  return !!ua && /iPhone|iPad|Android|Mobile/.test(ua)
}
