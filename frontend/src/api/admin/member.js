import request from '../request'

export function listAdminMembers(params) {
  return request.get('/admin/members', { params })
}

export function getAdminMember(id) {
  return request.get(`/admin/members/${id}`)
}

export function updateMemberStatus(id, status) {
  return request.patch(`/admin/members/${id}/status`, { status })
}
