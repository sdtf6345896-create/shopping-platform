import request from './request'

// { balance, earnRate, maxRedeemRatio }
export function getPointBalance() {
  return request.get('/points')
}

export function listPointTransactions(params) {
  return request.get('/points/transactions', { params })
}
