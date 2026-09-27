import request from './request'

// { tier, label, pointsMultiplier, spending, nextTier, nextLabel, amountToNext }
export function getMyTier() {
  return request.get('/members/me/tier')
}
