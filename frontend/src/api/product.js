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
