import request from './request'

export function listProducts(params) {
  return request.get('/products', { params })
}

export function getProductDetail(id) {
  return request.get(`/products/${id}`)
}
