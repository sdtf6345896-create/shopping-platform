import request from './request'

export function applyCoupon(data) {
  return request.post('/coupons/apply', data)
}
