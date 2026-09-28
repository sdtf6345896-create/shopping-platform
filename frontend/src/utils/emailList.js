// 把貼上的 Email 名單(換行、逗號、分號或空白分隔)整理成不重複的小寫清單
export function parseEmailList(text) {
  const seen = new Set()
  for (const part of (text || '').split(/[\s,;]+/)) {
    const email = part.trim().toLowerCase()
    if (email) seen.add(email)
  }
  return [...seen]
}
