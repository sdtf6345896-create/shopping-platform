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

export function getCategorySales(params) {
  return request.get('/admin/reports/categories', { params })
}

export function getDailySales(params) {
  return request.get('/admin/reports/daily', { params })
}

// 匯出報表 CSV;type: 'daily' | 'products' | 'categories'
export function exportReport(type, params) {
  return request.get('/admin/reports/export', { params: { type, ...params }, responseType: 'blob', timeout: 60000 })
}
