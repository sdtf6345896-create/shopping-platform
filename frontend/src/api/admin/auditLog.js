import request from '../request'

// params: { adminUsername?, targetType?, targetId?, page, size }
export function listAuditLogs(params) {
  return request.get('/admin/audit-logs', { params })
}
