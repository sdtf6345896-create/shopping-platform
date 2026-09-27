import request from './request'

// { personalized, products }
export function getRecommendations(params) {
  return request.get('/recommendations', { params })
}
