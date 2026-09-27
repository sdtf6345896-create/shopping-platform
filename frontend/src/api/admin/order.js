import request from '../request'

export function listAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

// 回傳完整 axios response(data 為 Blob),params 同列表查詢條件
export function exportAdminOrders(params) {
  return request.get('/admin/orders/export', { params, responseType: 'blob', timeout: 60000 })
}

export function getAdminOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

// payload: { status, shippingCarrier?, trackingNumber?, note? }
export function updateOrderStatus(id, payload) {
  return request.patch(`/admin/orders/${id}/status`, payload)
}
