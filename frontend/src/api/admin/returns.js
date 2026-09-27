import request from '../request'

// params: { status?: 'PENDING' | 'APPROVED' | 'REJECTED', page, size }
export function listReturns(params) {
  return request.get('/admin/returns', { params })
}

export function approveReturn(id, note) {
  return request.post(`/admin/returns/${id}/approve`, { note })
}

export function rejectReturn(id, note) {
  return request.post(`/admin/returns/${id}/reject`, { note })
}
