import request from '../request'

// params: { rating?, replied?, hidden?, keyword?, page, size }
export function listAdminReviews(params) {
  return request.get('/admin/reviews', { params })
}

// reply 為空字串表示刪除回覆
export function replyReview(id, reply) {
  return request.put(`/admin/reviews/${id}/reply`, { reply })
}

export function setReviewHidden(id, hidden) {
  return request.patch(`/admin/reviews/${id}/hidden`, { hidden })
}
