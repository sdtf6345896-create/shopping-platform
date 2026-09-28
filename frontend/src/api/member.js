import request from './request'

// { tier, label, pointsMultiplier, spending, nextTier, nextLabel, amountToNext }
export function getMyTier() {
  return request.get('/members/me/tier')
}

// 我的邀請碼與邀請成果(第一次查看時後端才產生邀請碼)
export function getMyReferral() {
  return request.get('/members/me/referral')
}
