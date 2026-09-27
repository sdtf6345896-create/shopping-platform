import request from '../request'

export function listAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

export function getAdminOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

// payload: { status, shippingCarrier?, trackingNumber?, note? }
export function updateOrderStatus(id, payload) {
  return request.patch(`/admin/orders/${id}/status`, payload)
}
