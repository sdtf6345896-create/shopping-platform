// 後端回傳的 LocalDateTime 沒有時區(例如 "2026-09-27T16:00:00"),依瀏覽器當地時間解析
export function parseLocalDateTime(value) {
  return value ? new Date(value) : null
}

/**
 * 計算距離期限的剩餘毫秒數,已過期回傳 0。
 */
export function remainingMs(deadline, now = Date.now()) {
  const target = parseLocalDateTime(deadline)
  if (!target) return 0
  return Math.max(target.getTime() - now, 0)
}

/**
 * 把毫秒轉成倒數字串:未滿一小時顯示 mm:ss,否則 h:mm:ss。
 */
export function formatCountdown(ms) {
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (n) => String(n).padStart(2, '0')
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`
}
