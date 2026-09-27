import request from './request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function refresh(refreshToken) {
  return request.post('/auth/refresh', { refreshToken })
}

export function logout(refreshToken) {
  return request.post('/auth/logout', { refreshToken })
}

export function verifyEmail(token) {
  return request.post('/auth/verify-email', { token })
}

export function resendVerification(email) {
  return request.post('/auth/resend-verification', { email })
}

export function forgotPassword(data) {
  return request.post('/auth/forgot-password', data)
}

export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}

export function getProfile() {
  return request.get('/members/me')
}

export function updateProfile(data) {
  return request.put('/members/me', data)
}
