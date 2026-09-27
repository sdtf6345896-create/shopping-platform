// 與後端 InvoiceRules 相同的電子發票格式規則,結帳時先在前端提示

export const INVOICE_TYPE_LABELS = {
  MEMBER_CARRIER: '會員載具',
  MOBILE_BARCODE: '手機條碼載具',
  COMPANY: '公司戶(統一編號)',
  DONATION: '捐贈發票',
}

export function isValidMobileBarcode(value) {
  return /^\/[0-9A-Z.+-]{7}$/.test(value ?? '')
}

export function isValidDonationCode(value) {
  return /^\d{3,7}$/.test(value ?? '')
}

// 統一編號檢查碼:權數 1,2,1,2,1,2,4,1,乘積十位與個位相加後加總,能被 5 整除即有效;
// 第 7 位為 7 時,乘積 28 → 10 可視為 1 或 0
export function isValidTaxId(value) {
  if (!/^\d{8}$/.test(value ?? '')) return false
  const weights = [1, 2, 1, 2, 1, 2, 4, 1]
  const sum = [...value].reduce((acc, digit, i) => {
    const product = Number(digit) * weights[i]
    return acc + Math.floor(product / 10) + (product % 10)
  }, 0)
  return sum % 5 === 0 || (value[6] === '7' && (sum - 1) % 5 === 0)
}

// 回傳錯誤訊息,沒問題則回傳 null
export function validateInvoice(invoice) {
  switch (invoice.type) {
    case 'MOBILE_BARCODE':
      return isValidMobileBarcode((invoice.carrierCode ?? '').trim().toUpperCase())
        ? null
        : '手機條碼格式不正確(斜線開頭加 7 碼英數字)'
    case 'COMPANY':
      if (!isValidTaxId((invoice.taxId ?? '').trim())) return '統一編號不正確'
      return (invoice.companyTitle ?? '').trim() ? null : '請填寫發票抬頭'
    case 'DONATION':
      return isValidDonationCode((invoice.donationCode ?? '').trim()) ? null : '愛心碼格式不正確(3 到 7 位數字)'
    default:
      return null
  }
}

// 顯示用摘要,例如「統編 04595257 範例公司」
export function describeInvoice(invoice) {
  if (!invoice) return INVOICE_TYPE_LABELS.MEMBER_CARRIER
  switch (invoice.type) {
    case 'MOBILE_BARCODE':
      return `手機條碼 ${invoice.carrierCode}`
    case 'COMPANY':
      return `統編 ${invoice.taxId} ${invoice.companyTitle}`
    case 'DONATION':
      return `捐贈(愛心碼 ${invoice.donationCode})`
    default:
      return INVOICE_TYPE_LABELS.MEMBER_CARRIER
  }
}
