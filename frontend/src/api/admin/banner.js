import request from '../request'

export function listAdminBanners() {
  return request.get('/admin/banners')
}

export function createBanner(data) {
  return request.post('/admin/banners', data)
}

export function updateBanner(id, data) {
  return request.put(`/admin/banners/${id}`, data)
}

export function updateBannerStatus(id, status) {
  return request.patch(`/admin/banners/${id}/status`, { status })
}

export function deleteBanner(id) {
  return request.delete(`/admin/banners/${id}`)
}
