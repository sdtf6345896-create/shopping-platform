import request from './request'

export function listQuestions(productId, params) {
  return request.get(`/products/${productId}/questions`, { params })
}

export function askQuestion(productId, content) {
  return request.post(`/products/${productId}/questions`, { content })
}
