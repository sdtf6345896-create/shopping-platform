import request from './request'

// 我在某商品已訂閱貨到通知的規格 id 陣列
export function listMyStockAlerts(productId) {
  return request.get('/stock-alerts', { params: { productId } })
}

export function subscribeStockAlert(skuId) {
  return request.post(`/stock-alerts/${skuId}`)
}

export function unsubscribeStockAlert(skuId) {
  return request.delete(`/stock-alerts/${skuId}`)
}
