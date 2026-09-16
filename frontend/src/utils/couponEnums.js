export const DISCOUNT_TYPE_LABELS = {
  FIXED_AMOUNT: '固定折抵',
  PERCENTAGE: '百分比折扣',
}

export const COUPON_STATUS_LABELS = {
  ACTIVE: '啟用',
  DISABLED: '停用',
}

export function formatDiscount(coupon) {
  if (coupon.discountType === 'PERCENTAGE') {
    const cap = coupon.maxDiscountAmount ? `,上限 NT$ ${coupon.maxDiscountAmount}` : ''
    return `${coupon.discountValue}% 折扣${cap}`
  }
  return `折抵 NT$ ${coupon.discountValue}`
}
