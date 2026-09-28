import request from '../request'

export function getMyAdminAccount() {
  return request.get('/admin/account/me')
}

export function changeMyAdminPassword(currentPassword, newPassword) {
  return request.put('/admin/account/password', { currentPassword, newPassword })
}

export function listAdminAccounts() {
  return request.get('/admin/accounts')
}

export function createAdminAccount(data) {
  return request.post('/admin/accounts', data)
}

export function updateAdminAccount(id, data) {
  return request.put(`/admin/accounts/${id}`, data)
}

export function resetAdminPassword(id, newPassword) {
  return request.put(`/admin/accounts/${id}/password`, { newPassword })
}
