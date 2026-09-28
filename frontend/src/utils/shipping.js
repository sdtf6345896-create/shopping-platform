export const SHIPPING_METHOD_LABELS = {
  HOME_DELIVERY: '宅配到府',
  CVS_PICKUP: '超商取貨',
}

export const CVS_BRANDS = [
  { value: 'SEVEN_ELEVEN', label: '7-ELEVEN' },
  { value: 'FAMILY_MART', label: '全家' },
  { value: 'HI_LIFE', label: '萊爾富' },
  { value: 'OK_MART', label: 'OK 超商' },
]

// 與後端 ShippingPolicy 相同的規則:商品金額(套用優惠券後)達門檻免運,否則依配送方式收固定運費
export function shippingFeeFor(amount, policy, method = 'HOME_DELIVERY') {
  if (!policy || amount <= 0) return 0
  if (amount >= Number(policy.freeThreshold)) return 0
  return Number(method === 'CVS_PICKUP' ? policy.cvsFee : policy.fee)
}

// 與後端 CvsPickupRequest 相同的檢查,回傳第一個錯誤訊息(沒問題回 null)
export function validateCvsPickup(pickup) {
  if (!pickup.brand) return '請選擇取貨超商'
  if (!pickup.storeName.trim()) return '請填寫取貨門市'
  if (pickup.storeName.trim().length > 30) return '門市名稱最多 30 字'
  if (!/^[0-9]{0,8}$/.test(pickup.storeCode.trim())) return '店號只能是數字(最多 8 碼)'
  if (!pickup.recipientName.trim()) return '請填寫取件人姓名'
  if (!/^09[0-9]{8}$/.test(pickup.recipientPhone.trim())) return '取件人手機格式應為 09 開頭的 10 碼數字'
  return null
}

// 距離免運還差多少(已達門檻為 0)
export function amountToFreeShipping(amount, policy) {
  if (!policy) return 0
  return Math.max(0, Number(policy.freeThreshold) - amount)
}
