<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrder, updateOrderStatus } from '../../api/admin/order'
import {
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TAG_TYPES,
  ORDER_STATUS_TRANSITIONS,
  PAYMENT_METHOD_LABELS,
} from '../../utils/orderEnums'

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
    order.value = await getAdminOrder(props.id)
  } finally {
    loading.value = false
  }
}

async function handleTransition(action) {
  if (action.status === 'CANCELLED') {
    try {
      await ElMessageBox.confirm('確定要取消此訂單嗎?', '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  acting.value = true
  try {
    await updateOrderStatus(props.id, action.status)
    ElMessage.success('狀態更新成功')
    await load()
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <router-link :to="{ name: 'AdminOrderList' }" class="back-link">← 返回訂單管理</router-link>

    <template v-if="order">
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
        <div class="item-row">
          <span>小計</span>
          <span>NT$ {{ order.subtotalAmount }}</span>
        </div>
        <div v-if="order.discountAmount > 0" class="item-row discount-row">
          <span>優惠折抵{{ order.couponCode ? `(${order.couponCode})` : '' }}</span>
          <span>- NT$ {{ order.discountAmount }}</span>
        </div>
        <div class="total-row">
          <span>總金額</span>
          <span class="total-amount">NT$ {{ order.totalAmount }}</span>
        </div>
      </div>

      <div class="actions">
        <el-button
          v-for="action in ORDER_STATUS_TRANSITIONS[order.status]"
          :key="action.status"
          :type="action.type"
          :loading="acting"
          @click="handleTransition(action)"
        >
          {{ action.label }}
        </el-button>
        <span v-if="ORDER_STATUS_TRANSITIONS[order.status]?.length === 0" class="no-action-hint">
          此訂單已無可執行的狀態變更
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped>
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
  max-width: 600px;
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

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.no-action-hint {
  color: #999;
  font-size: 13px;
}
</style>
