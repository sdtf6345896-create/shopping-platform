import request from '../request'

export function getSalesSummary(params) {
  return request.get('/admin/reports/summary', { params })
}

export function getTopProducts(params) {
  return request.get('/admin/reports/top-products', { params })
}

export function getDashboard() {
  return request.get('/admin/reports/dashboard')
}

export function getDailySales(params) {
  return request.get('/admin/reports/daily', { params })
}
