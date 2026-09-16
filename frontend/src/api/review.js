import request from './request'

export function listReviews(productId, params) {
  return request.get(`/products/${productId}/reviews`, { params })
}

export function getReviewSummary(productId) {
  return request.get(`/products/${productId}/reviews/summary`)
}

export function getMyReview(productId) {
  return request.get(`/products/${productId}/reviews/me`)
}

export function upsertMyReview(productId, data) {
  return request.put(`/products/${productId}/reviews/me`, data)
}

export function deleteMyReview(productId) {
  return request.delete(`/products/${productId}/reviews/me`)
}
