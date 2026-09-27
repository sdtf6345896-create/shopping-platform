// 從 Content-Disposition 取出檔名,支援 RFC 5987 的 filename*=UTF-8''xxx
export function filenameFromDisposition(header, fallback) {
  if (!header) return fallback
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header)
  if (encoded) return decodeURIComponent(encoded[1])
  const plain = /filename="?([^";]+)"?/i.exec(header)
  return plain ? plain[1] : fallback
}

// 把 Blob 存成檔案(建立暫時的 <a download> 觸發瀏覽器下載)
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
