import request from '../request'

export function listAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

export function getAdminOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

export function updateOrderStatus(id, status) {
  return request.patch(`/admin/orders/${id}/status`, { status })
}
