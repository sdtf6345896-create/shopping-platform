import request from './request'

export function checkout(data) {
  return request.post('/orders', data)
}

export function listMyOrders(params) {
  return request.get('/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

export function payOrder(id) {
  return request.post(`/orders/${id}/pay`)
}

export function reorder(id) {
  return request.post(`/orders/${id}/reorder`)
}

export function applyReturn(id, reason) {
  return request.post(`/orders/${id}/return`, { reason })
}

export function confirmReceipt(id) {
  return request.post(`/orders/${id}/complete`)
}

export function cancelOrder(id, reason) {
  return request.post(`/orders/${id}/cancel`, { reason: reason || null })
}

export function getOrderMessages(orderId) {
  return request.get(`/orders/${orderId}/messages`)
}

export function postOrderMessage(orderId, content) {
  return request.post(`/orders/${orderId}/messages`, { content })
}
