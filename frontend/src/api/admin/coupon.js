import request from '../request'

export function listAdminCoupons(params) {
  return request.get('/admin/coupons', { params })
}

export function getAdminCoupon(id) {
  return request.get(`/admin/coupons/${id}`)
}

export function createCoupon(data) {
  return request.post('/admin/coupons', data)
}

export function updateCoupon(id, data) {
  return request.put(`/admin/coupons/${id}`, data)
}

export function updateCouponStatus(id, status) {
  return request.patch(`/admin/coupons/${id}/status`, { status })
}

export function deleteCoupon(id) {
  return request.delete(`/admin/coupons/${id}`)
}

// 直接發券到會員錢包:payload = { target: 'ALL' | 'TIER' | 'EMAILS', minTier, emails }
export function issueCoupon(id, payload) {
  return request.post(`/admin/coupons/${id}/issue`, payload, { timeout: 60000 })
}
