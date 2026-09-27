<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrder, updateOrderStatus } from '../../api/admin/order'
import {
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TAG_TYPES,
  ORDER_STATUS_TRANSITIONS,
  PAYMENT_METHOD_LABELS,
  RETURN_STATUS_LABELS,
  RETURN_STATUS_TAG_TYPES,
  SHIPPING_CARRIERS,
} from '../../utils/orderEnums'
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
    order.value = await getAdminOrder(props.id)
  } finally {
    loading.value = false
  }
}

const shipDialogVisible = ref(false)
const shipForm = reactive({ shippingCarrier: SHIPPING_CARRIERS[0], trackingNumber: '' })

async function submitStatus(payload) {
  acting.value = true
  try {
    await updateOrderStatus(props.id, payload)
    ElMessage.success('狀態更新成功')
    await load()
    return true
  } catch {
    return false
  } finally {
    acting.value = false
  }
}

async function handleTransition(action) {
  if (action.status === 'SHIPPING') {
    shipForm.trackingNumber = ''
    shipDialogVisible.value = true
    return
  }
  let note
  if (action.status === 'CANCELLED') {
    try {
      const { value } = await ElMessageBox.prompt('確定要取消此訂單嗎?可填寫取消原因(選填)', '取消訂單', {
        type: 'warning',
        inputPlaceholder: '例如:商品缺貨',
        inputValidator: (v) => !v || v.length <= 255 || '最多 255 字',
      })
      note = value
    } catch {
      return
    }
  }
  await submitStatus({ status: action.status, note })
}

async function handleShip() {
  if (!shipForm.shippingCarrier?.trim() || !shipForm.trackingNumber.trim()) {
    ElMessage.warning('請填寫物流業者與物流單號')
    return
  }
  if (await submitStatus({ status: 'SHIPPING', ...shipForm })) {
    shipDialogVisible.value = false
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
        <template v-if="order.trackingNumber">
          <p class="meta">物流:{{ order.shippingCarrier }} / {{ order.trackingNumber }}</p>
          <p class="meta">出貨時間:{{ order.shippedAt?.slice(0, 19).replace('T', ' ') }}</p>
        </template>
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
        <p class="meta">退貨原因:{{ order.returnRequest.reason }}</p>
        <p v-if="order.returnRequest.adminNote" class="meta">處理說明:{{ order.returnRequest.adminNote }}</p>
        <router-link
          v-if="order.returnRequest.status === 'PENDING'"
          :to="{ name: 'AdminReturnList' }"
          class="meta"
        >
          前往退貨管理審核 →
        </router-link>
      </div>

      <div class="block">
        <div class="block-title">狀態歷程</div>
        <OrderTimeline :logs="order.statusLogs" show-actor />
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
        <el-button v-if="['PAID', 'SHIPPING'].includes(order.status)" @click="router.push({ name: 'AdminPackingSlip', params: { id: order.id } })">
          列印揀貨單
        </el-button>
        <span v-if="ORDER_STATUS_TRANSITIONS[order.status]?.length === 0" class="no-action-hint">
          此訂單已無可執行的狀態變更
        </span>
      </div>
    </template>

    <el-dialog v-model="shipDialogVisible" title="填寫出貨資訊" width="420px">
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="物流業者" required>
          <el-select v-model="shipForm.shippingCarrier" filterable allow-create style="width: 100%">
            <el-option v-for="c in SHIPPING_CARRIERS" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流單號" required>
          <el-input v-model="shipForm.trackingNumber" maxlength="50" placeholder="請輸入物流追蹤單號" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="handleShip">確認出貨</el-button>
      </template>
    </el-dialog>
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
