import request from './request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function getProfile() {
  return request.get('/members/me')
}

export function updateProfile(data) {
  return request.put('/members/me', data)
}
