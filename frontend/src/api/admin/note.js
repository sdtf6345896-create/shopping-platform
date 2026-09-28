import request from '../request'

// targetType: 'ORDER' | 'MEMBER'
export function listAdminNotes(targetType, targetId) {
  return request.get('/admin/notes', { params: { targetType, targetId } })
}

export function addAdminNote(payload) {
  return request.post('/admin/notes', payload)
}

export function deleteAdminNote(id) {
  return request.delete(`/admin/notes/${id}`)
}
