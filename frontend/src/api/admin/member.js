import request from '../request'

export function listAdminMembers(params) {
  return request.get('/admin/members', { params })
}

export function getAdminMember(id) {
  return request.get(`/admin/members/${id}`)
}

// amount 正數發放、負數扣除
export function adjustMemberPoints(id, amount, reason) {
  return request.post(`/admin/members/${id}/points`, { amount, reason })
}

export function updateMemberStatus(id, status) {
  return request.patch(`/admin/members/${id}/status`, { status })
}
