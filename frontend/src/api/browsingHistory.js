import request from './request'

export function listBrowsingHistory(params) {
  return request.get('/browsing-history', { params })
}

export function recordView(productId) {
  return request.post(`/browsing-history/${productId}`)
}

export function removeFromBrowsingHistory(productId) {
  return request.delete(`/browsing-history/${productId}`)
}

export function clearBrowsingHistory() {
  return request.delete('/browsing-history')
}
