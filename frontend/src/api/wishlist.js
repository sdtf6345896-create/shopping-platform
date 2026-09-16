import request from './request'

export function listWishlist(params) {
  return request.get('/wishlist', { params })
}

export function isFavorited(productId) {
  return request.get(`/wishlist/${productId}`)
}

export function addToWishlist(productId) {
  return request.post(`/wishlist/${productId}`)
}

export function removeFromWishlist(productId) {
  return request.delete(`/wishlist/${productId}`)
}
