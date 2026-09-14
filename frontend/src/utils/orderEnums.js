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
