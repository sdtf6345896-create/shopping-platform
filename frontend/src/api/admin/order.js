import request from '../request'

export function listAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

// 回傳完整 axios response(data 為 Blob),params 同列表查詢條件
export function exportAdminOrders(params) {
  return request.get('/admin/orders/export', { params, responseType: 'blob', timeout: 60000 })
}

// 所有待出貨訂單的出貨單號範本(填好物流欄位即可上傳)
export function downloadShipTemplate() {
  return request.get('/admin/orders/ship-template', { responseType: 'blob', timeout: 60000 })
}

export function importShipCsv(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/orders/ship-import', formData, { timeout: 60000 })
}

export function getAdminOrder(id) {
  return request.get(`/admin/orders/${id}`)
}

// payload: { status, shippingCarrier?, trackingNumber?, note? }
export function updateOrderStatus(id, payload) {
  return request.patch(`/admin/orders/${id}/status`, payload)
}

// 最後一則是買家留言、等待回覆的訂單
export function listAwaitingOrderMessages(params) {
  return request.get('/admin/order-messages/awaiting', { params })
}

export function listAdminOrderMessages(orderId) {
  return request.get(`/admin/orders/${orderId}/messages`)
}

export function replyOrderMessage(orderId, content) {
  return request.post(`/admin/orders/${orderId}/messages`, { content })
}
