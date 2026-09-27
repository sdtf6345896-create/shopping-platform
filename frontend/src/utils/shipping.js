// 與後端 ShippingPolicy 相同的規則:商品金額(套用優惠券後)達門檻免運,否則收固定運費
export function shippingFeeFor(amount, policy) {
  if (!policy || amount <= 0) return 0
  return amount >= Number(policy.freeThreshold) ? 0 : Number(policy.fee)
}

// 距離免運還差多少(已達門檻為 0)
export function amountToFreeShipping(amount, policy) {
  if (!policy) return 0
  return Math.max(0, Number(policy.freeThreshold) - amount)
}
