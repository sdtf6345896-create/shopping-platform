<script setup>
import { describeInvoice } from '../../utils/invoice'
import { computed, h, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyReturn, confirmReceipt, getOrderDetail, payOrder, cancelOrder, reorder } from '../../api/order'
import { useCartStore } from '../../stores/cart'
import {
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TAG_TYPES,
  PAYMENT_METHOD_LABELS,
  RETURN_STATUS_LABELS,
  RETURN_STATUS_TAG_TYPES,
} from '../../utils/orderEnums'
import { formatCountdown, remainingMs } from '../../utils/countdown'
import OrderTimeline from '../../components/OrderTimeline.vue'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()
const order = ref(null)
const loading = ref(true)
const acting = ref(false)

async function load() {
  loading.value = true
  try {
    order.value = await getOrderDetail(props.id)
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  acting.value = true
  try {
    await payOrder(props.id)
    ElMessage.success('付款成功(模擬)')
    await load()
  } finally {
    acting.value = false
  }
}

async function handleCancel() {
  await ElMessageBox.confirm('確定要取消此訂單嗎?', '提示', { type: 'warning' })
  acting.value = true
  try {
    await cancelOrder(props.id)
    ElMessage.success('訂單已取消')
    await load()
  } finally {
    acting.value = false
  }
}

const cartStore = useCartStore()

async function handleReorder() {
  acting.value = true
  let result
  try {
    result = await reorder(props.id)
    await cartStore.fetchCart()
  } finally {
    acting.value = false
  }

  const message = (headline) =>
    h('div', [h('p', headline), ...result.notices.map((n) => h('p', { class: 'reorder-notice' }, n))])
  if (result.addedCount === 0) {
    ElMessageBox.alert(message('沒有商品可以加入購物車'), '再買一次', { type: 'warning' })
    return
  }
  try {
    await ElMessageBox.confirm(message(`已將 ${result.addedCount} 項商品加入購物車`), '再買一次', {
      type: result.notices.length ? 'warning' : 'success',
      confirmButtonText: '前往購物車',
      cancelButtonText: '繼續逛逛',
    })
    router.push('/cart')
  } catch {
    // 留在本頁
  }
}

// 付款期限倒數(貨到付款沒有期限)
const now = ref(Date.now())
const timer = setInterval(() => {
  now.value = Date.now()
}, 1000)
onUnmounted(() => clearInterval(timer))

// 已完成、還在鑑賞期內、沒申請過才能申請退貨
const canApplyReturn = computed(
  () =>
    order.value?.status === 'COMPLETED' &&
    !order.value.returnRequest &&
    order.value.returnDeadline &&
    remainingMs(order.value.returnDeadline, now.value) > 0,
)

async function handleConfirmReceipt() {
  try {
    await ElMessageBox.confirm('確認已收到商品?確認後訂單完成並回饋購物金。', '確認收貨', { type: 'info' })
  } catch {
    return
  }
  acting.value = true
  try {
    order.value = await confirmReceipt(props.id)
    ElMessage.success('已確認收貨,感謝您的購買!')
  } finally {
    acting.value = false
  }
}

async function handleApplyReturn() {
  let reason
  try {
    const { value } = await ElMessageBox.prompt(
      `請說明退貨原因(鑑賞期至 ${order.value.returnDeadline.slice(0, 16).replace('T', ' ')})`,
      '申請退貨',
      {
        inputType: 'textarea',
        inputPlaceholder: '例如:尺寸不合、商品瑕疵',
        inputValidator: (v) => (!!v && !!v.trim() && v.length <= 500) || '請填寫原因(最多 500 字)',
      },
    )
    reason = value
  } catch {
    return
  }
  acting.value = true
  try {
    order.value = await applyReturn(props.id, reason)
    ElMessage.success('退貨申請已送出,審核結果會以 email 通知')
  } finally {
    acting.value = false
  }
}

const awaitingPayment = computed(() => order.value?.status === 'PENDING_PAYMENT')
const hasDeadline = computed(() => awaitingPayment.value && !!order.value?.paymentDeadline)
const timeLeft = computed(() => (hasDeadline.value ? remainingMs(order.value.paymentDeadline, now.value) : 0))
const paymentExpired = computed(() => hasDeadline.value && timeLeft.value === 0)

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page-container order-detail-page">
    <template v-if="order">
      <router-link to="/member/orders" class="back-link">← 返回我的訂單</router-link>

      <div class="block">
        <div class="block-title">
          <span>訂單 {{ order.orderNo }}</span>
          <el-tag :type="ORDER_STATUS_TAG_TYPES[order.status]">{{ ORDER_STATUS_LABELS[order.status] }}</el-tag>
        </div>
        <p class="meta">建立時間:{{ order.createdAt?.slice(0, 19).replace('T', ' ') }}</p>
        <p class="meta">付款方式:{{ PAYMENT_METHOD_LABELS[order.paymentMethod] }}</p>
        <p class="meta">收件人:{{ order.receiverName }} {{ order.receiverPhone }}</p>
        <p class="meta">收件地址:{{ order.receiverAddress }}</p>
        <p class="meta">發票:{{ describeInvoice(order.invoice) }}</p>
        <p v-if="order.buyerNote" class="meta buyer-note">訂單備註:{{ order.buyerNote }}</p>
      </div>

      <div v-if="order.trackingNumber" class="block">
        <div class="block-title">物流資訊</div>
        <p class="meta">物流業者:{{ order.shippingCarrier }}</p>
        <p class="meta">物流單號:<span class="tracking-no">{{ order.trackingNumber }}</span></p>
        <p class="meta">出貨時間:{{ order.shippedAt?.slice(0, 19).replace('T', ' ') }}</p>
        <p v-if="order.status === 'SHIPPING'" class="meta auto-hint">收到商品後請按「確認收貨」;出貨 7 天後未確認將自動完成</p>
      </div>

      <div class="block">
        <div class="block-title">商品明細</div>
        <div v-for="item in order.items" :key="item.id" class="item-row">
          <span>{{ item.productName }} - {{ item.specName }} x {{ item.quantity }}</span>
          <span>NT$ {{ item.subtotal }}</span>
        </div>
        <div class="item-row">
          <span>小計</span>
          <span>NT$ {{ order.subtotalAmount }}</span>
        </div>
        <div v-if="order.discountAmount > 0" class="item-row discount-row">
          <span>優惠折抵{{ order.couponCode ? `(${order.couponCode})` : '' }}</span>
          <span>- NT$ {{ order.discountAmount }}</span>
        </div>
        <div v-if="order.pointsUsed > 0" class="item-row discount-row">
          <span>購物金折抵</span>
          <span>- NT$ {{ order.pointsUsed }}</span>
        </div>
        <div class="item-row">
          <span>運費</span>
          <span>{{ order.shippingFee > 0 ? `NT$ ${order.shippingFee}` : '免運' }}</span>
        </div>
        <div class="total-row">
          <span>總金額</span>
          <span class="total-amount">NT$ {{ order.totalAmount }}</span>
        </div>
      </div>

      <div v-if="order.returnRequest" class="block">
        <div class="block-title">
          <span>退貨申請</span>
          <el-tag :type="RETURN_STATUS_TAG_TYPES[order.returnRequest.status]">
            {{ RETURN_STATUS_LABELS[order.returnRequest.status] }}
          </el-tag>
        </div>
        <p class="meta">申請時間:{{ order.returnRequest.createdAt?.slice(0, 19).replace('T', ' ') }}</p>
        <p class="meta">退貨原因:{{ order.returnRequest.reason }}</p>
        <p v-if="order.returnRequest.adminNote" class="meta">處理說明:{{ order.returnRequest.adminNote }}</p>
      </div>

      <div class="block">
        <div class="block-title">訂單進度</div>
        <OrderTimeline :logs="order.statusLogs" />
      </div>

      <el-alert
        v-if="hasDeadline"
        class="deadline-alert"
        :type="paymentExpired ? 'error' : 'warning'"
        :closable="false"
        show-icon
      >
        <template v-if="paymentExpired">已超過付款期限,訂單將由系統自動取消</template>
        <template v-else>
          請在 <strong class="countdown">{{ formatCountdown(timeLeft) }}</strong> 內完成付款,逾期訂單將自動取消
        </template>
      </el-alert>

      <div class="actions">
        <el-button
          v-if="awaitingPayment"
          type="primary"
          :loading="acting"
          :disabled="paymentExpired"
          @click="handlePay"
        >
          模擬付款
        </el-button>
        <el-button
          v-if="['PENDING_PAYMENT', 'PAID'].includes(order.status)"
          :loading="acting"
          @click="handleCancel"
        >
          取消訂單
        </el-button>
        <el-button v-if="canApplyReturn" :loading="acting" @click="handleApplyReturn">申請退貨</el-button>
        <el-button v-if="order.status === 'SHIPPING'" type="primary" :loading="acting" @click="handleConfirmReceipt">
          確認收貨
        </el-button>
        <el-button :loading="acting" @click="handleReorder">再買一次</el-button>
        <el-button v-if="!['PENDING_PAYMENT', 'CANCELLED'].includes(order.status)" @click="router.push({ name: 'OrderReceipt', params: { id: order.id } })">
          列印收據
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.buyer-note {
  color: #e6a23c;
  white-space: pre-wrap;
}

.order-detail-page {
  max-width: 720px;
}

.back-link {
  display: inline-block;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.item-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px dashed #eee;
}

.total-row {
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
  font-weight: 600;
}

.total-amount {
  color: #e4393c;
  font-size: 18px;
}

.discount-row {
  color: #e4393c;
}

.auto-hint {
  color: #e6a23c;
}

.tracking-no {
  font-family: monospace;
  color: #333;
}

.deadline-alert {
  margin-bottom: 16px;
}

.countdown {
  font-variant-numeric: tabular-nums;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>

<style>
/* ElMessageBox 掛在 body 底下,scoped 樣式套不到 */
.reorder-notice {
  color: #e6a23c;
  font-size: 13px;
  margin-top: 4px;
}
</style>
