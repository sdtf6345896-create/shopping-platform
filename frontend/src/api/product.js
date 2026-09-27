import request from './request'

export function listProducts(params) {
  return request.get('/products', { params })
}

export function getProductDetail(id) {
  return request.get(`/products/${id}`)
}

export function listFlashSaleProducts(params) {
  return request.get('/products/flash-sale', { params })
}

export function listRelatedProducts(id, params) {
  return request.get(`/products/${id}/related`, { params })
}

export function suggestProducts(keyword, limit = 8) {
  return request.get('/products/suggestions', { params: { keyword, limit } })
}

// [{ product, orderCount }]
export function listBoughtTogether(id, params) {
  return request.get(`/products/${id}/bought-together`, { params })
}

export function getHotSearches(limit = 8) {
  return request.get('/products/hot-searches', { params: { limit } })
}
