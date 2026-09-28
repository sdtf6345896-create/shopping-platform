// 與後端 PointPolicy 相同的規則:最多折抵「應付金額 × 比例」(無條件捨去),且不超過餘額
export function maxRedeemable(balance, payableAmount, ratio) {
  if (!balance || !payableAmount || payableAmount <= 0) return 0
  const cap = Math.floor(payableAmount * ratio)
  return Math.max(0, Math.min(balance, cap))
}

export const POINT_TYPE_LABELS = {
  EARN: '訂單回饋',
  REDEEM: '結帳折抵',
  REFUND: '取消退還',
  BIRTHDAY: '生日禮',
  ADJUST: '活動贈送',
}
