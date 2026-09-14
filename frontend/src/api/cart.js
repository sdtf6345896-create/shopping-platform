import request from './request'

export function getCart() {
  return request.get('/cart')
}

export function addCartItem(data) {
  return request.post('/cart/items', data)
}

export function updateCartItem(itemId, quantity) {
  return request.put(`/cart/items/${itemId}`, { quantity })
}

export function removeCartItem(itemId) {
  return request.delete(`/cart/items/${itemId}`)
}

export function clearCart() {
  return request.delete('/cart')
}
