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

// 評價「有幫助」投票
export function voteReviewHelpful(productId, reviewId) {
  return request.post(`/products/${productId}/reviews/${reviewId}/helpful`)
}

export function unvoteReviewHelpful(productId, reviewId) {
  return request.delete(`/products/${productId}/reviews/${reviewId}/helpful`)
}

// 自己在這個商品按過「有幫助」的評價 id(未登入為空陣列)
export function getMyHelpfulVotes(productId) {
  return request.get(`/products/${productId}/reviews/helpful/me`)
}
