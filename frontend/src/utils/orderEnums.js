export const ORDER_STATUS_LABELS = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  SHIPPING: '出貨中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export const ORDER_STATUS_TAG_TYPES = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  SHIPPING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
}

export const PAYMENT_METHOD_LABELS = {
  CREDIT_CARD: '信用卡',
  ATM: 'ATM 轉帳',
  COD: '貨到付款',
}

// 對應後端 OrderServiceImpl 的合法狀態轉換,後台只呈現允許的下一步動作
export const ORDER_STATUS_TRANSITIONS = {
  PENDING_PAYMENT: [
    { status: 'PAID', label: '標記已付款', type: 'primary' },
    { status: 'CANCELLED', label: '取消訂單', type: 'danger' },
  ],
  PAID: [
    { status: 'SHIPPING', label: '出貨', type: 'primary' },
    { status: 'CANCELLED', label: '取消訂單', type: 'danger' },
  ],
  SHIPPING: [{ status: 'COMPLETED', label: '標記完成', type: 'success' }],
  COMPLETED: [],
  CANCELLED: [],
}

export const ORDER_ACTOR_LABELS = {
  MEMBER: '會員',
  ADMIN: '管理員',
  SYSTEM: '系統',
}

// 後台出貨時可選的物流業者(也可自行輸入)
export const SHIPPING_CARRIERS = ['黑貓宅急便', '新竹物流', '中華郵政', '7-11 交貨便', '全家店到店']
