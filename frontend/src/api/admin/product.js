import request from '../request'

export function listAdminProducts(params) {
  return request.get('/admin/products', { params })
}

export function getAdminProduct(id) {
  return request.get(`/admin/products/${id}`)
}

export function createProduct(data) {
  return request.post('/admin/products', data)
}

export function updateProduct(id, data) {
  return request.put(`/admin/products/${id}`, data)
}

export function updateProductStatus(id, status) {
  return request.patch(`/admin/products/${id}/status`, { status })
}

export function updateProductStatusBatch(ids, status) {
  return request.patch('/admin/products/status', { ids, status })
}

// 回傳 { applied, totalRows, updated, errors: [{ line, message }] }
export function importStockCsv(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/products/stock-import', formData, { timeout: 60000 })
}

export function updateSkuStock(productId, skuId, stock) {
  return request.patch(`/admin/products/${productId}/skus/${skuId}/stock`, { stock })
}

// params: { threshold?, limit? }
export function listLowStock(params) {
  return request.get('/admin/products/low-stock', { params })
}

export function deleteProduct(id) {
  return request.delete(`/admin/products/${id}`)
}

// 某規格的庫存異動紀錄,新到舊
export function listStockMovements(productId, skuId, params) {
  return request.get(`/admin/products/${productId}/skus/${skuId}/stock-movements`, { params })
}
