import request from '../request'

export function listAdminCategories() {
  return request.get('/admin/categories')
}

export function createCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}

export function updateCategoryStatus(id, status) {
  return request.patch(`/admin/categories/${id}/status`, { status })
}

export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}
