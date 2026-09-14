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

export function updateSkuStock(productId, skuId, stock) {
  return request.patch(`/admin/products/${productId}/skus/${skuId}/stock`, { stock })
}

export function deleteProduct(id) {
  return request.delete(`/admin/products/${id}`)
}
