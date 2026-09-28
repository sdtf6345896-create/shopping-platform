import request from './request'

// 進行中的滿件折扣(公開)
export function listPromotions() {
  return request.get('/promotions')
}

// 以購物車試算滿件折扣;cartItemIds 不帶表示整個購物車
export function previewCartPromotion(cartItemIds) {
  return request.get('/cart/promotion', {
    params: { cartItemIds: cartItemIds?.length ? cartItemIds.join(',') : undefined },
  })
}
