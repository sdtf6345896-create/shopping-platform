import request from '../request'

export function adminLogin(data) {
  return request.post('/admin/auth/login', data)
}
