import request from './request'

// 登入裝置管理
export function listMySessions() {
  return request.get('/members/me/sessions')
}

export function revokeSession(sessionId) {
  return request.delete(`/members/me/sessions/${sessionId}`)
}

export function revokeOtherSessions(currentSessionId) {
  return request.post('/members/me/sessions/revoke-others', { currentSessionId })
}
