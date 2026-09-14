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

export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`)
}
