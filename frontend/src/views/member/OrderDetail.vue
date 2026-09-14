<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, payOrder, cancelOrder } from '../../api/order'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TAG_TYPES, PAYMENT_METHOD_LABELS } from '../../utils/orderEnums'

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
      </div>

      <div class="block">
        <div class="block-title">商品明細</div>
        <div v-for="item in order.items" :key="item.id" class="item-row">
          <span>{{ item.productName }} - {{ item.specName }} x {{ item.quantity }}</span>
          <span>NT$ {{ item.subtotal }}</span>
        </div>
        <div class="total-row">
          <span>總金額</span>
          <span class="total-amount">NT$ {{ order.totalAmount }}</span>
        </div>
      </div>

      <div class="actions">
        <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" :loading="acting" @click="handlePay">
          模擬付款
        </el-button>
        <el-button
          v-if="['PENDING_PAYMENT', 'PAID'].includes(order.status)"
          :loading="acting"
          @click="handleCancel"
        >
          取消訂單
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
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

.actions {
  display: flex;
  gap: 12px;
}
</style>
