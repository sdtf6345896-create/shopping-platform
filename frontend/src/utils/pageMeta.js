export const SITE_NAME = 'MomoShop 購物平台'
export const DEFAULT_DESCRIPTION = 'MomoShop 購物平台:服飾、3C 家電、生活居家好物,滿額免運、超商取貨、限時特價。'

// 頁面標題:「商品名稱 | 網站名稱」,沒有標題時只顯示網站名稱
export function setPageTitle(title) {
  document.title = title ? `${title} | ${SITE_NAME}` : SITE_NAME
}

// 設定 <meta name="description">:去掉多餘空白並截到 150 字(搜尋結果摘要的合理長度)
export function setMetaDescription(text) {
  const normalized = (text || '').replace(/\s+/g, ' ').trim()
  const content = normalized ? normalized.slice(0, 150) : DEFAULT_DESCRIPTION
  let tag = document.querySelector('meta[name="description"]')
  if (!tag) {
    tag = document.createElement('meta')
    tag.setAttribute('name', 'description')
    document.head.appendChild(tag)
  }
  tag.setAttribute('content', content)
}
