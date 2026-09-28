import request from '../request'

export function listAdminPromotions() {
  return request.get('/admin/promotions')
}

export function createPromotion(data) {
  return request.post('/admin/promotions', data)
}

export function updatePromotion(id, data) {
  return request.put(`/admin/promotions/${id}`, data)
}

export function setPromotionActive(id, active) {
  return request.patch(`/admin/promotions/${id}/active`, { active })
}

export function deletePromotion(id) {
  return request.delete(`/admin/promotions/${id}`)
}
