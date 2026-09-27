import request from './request'

export function applyCoupon(data) {
  return request.post('/coupons/apply', data)
}

export function getCouponCenter() {
  return request.get('/coupons/center')
}

export function claimCoupon(id) {
  return request.post(`/coupons/${id}/claim`)
}

export function getMyCoupons() {
  return request.get('/coupons/mine')
}
