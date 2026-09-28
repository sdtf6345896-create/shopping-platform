import { describe, expect, it } from 'vitest'
import { canAccessAdminRoute } from './adminPermissions'

describe('canAccessAdminRoute', () => {
  it('lets admins everywhere', () => {
    expect(canAccessAdminRoute('ADMIN', 'AdminCouponList')).toBe(true)
  })

  it('limits staff to customer-service pages', () => {
    expect(canAccessAdminRoute('STAFF', 'AdminOrderDetail')).toBe(true)
    expect(canAccessAdminRoute('STAFF', 'AdminReviewList')).toBe(true)
    expect(canAccessAdminRoute('STAFF', 'AdminCouponList')).toBe(false)
    expect(canAccessAdminRoute('STAFF', 'AdminAccountList')).toBe(false)
    expect(canAccessAdminRoute('STAFF', 'AdminProductEdit')).toBe(false)
  })

  it('does not block before the role is known', () => {
    expect(canAccessAdminRoute(null, 'AdminCouponList')).toBe(true)
  })
})
