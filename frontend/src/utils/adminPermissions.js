// 客服(STAFF)可進入的後台頁面;其餘頁面只有 ADMIN 可用(需與後端 SecurityConfig 的規則一致)
const STAFF_ROUTES = new Set([
  'AdminDashboard',
  'AdminProductList',
  'AdminOrderList',
  'AdminOrderDetail',
  'AdminPackingSlip',
  'AdminReturnList',
  'AdminQuestionList',
  'AdminReviewList',
  'AdminOrderMessageList',
  'AdminMemberList',
  'AdminMemberDetail',
])

export const ROLE_LABELS = { ADMIN: '管理員', STAFF: '客服' }

// role 還不知道(例如剛重新整理)時先放行,由後端 403 把關,取得角色後選單會自動更新
export function canAccessAdminRoute(role, routeName) {
  if (!role || role === 'ADMIN') return true
  return STAFF_ROUTES.has(routeName)
}
