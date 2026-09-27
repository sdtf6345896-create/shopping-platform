import request from './request'

export function listNotifications(params) {
  return request.get('/notifications', { params })
}

export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

export function markNotificationRead(id) {
  return request.post(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.post('/notifications/read-all')
}
