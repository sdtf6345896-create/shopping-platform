<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listMyOrders } from '../../api/order'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TAG_TYPES } from '../../utils/orderEnums'

const router = useRouter()
const orders = ref([])
const total = ref(0)
const loading = ref(true)
const page = ref(0)
const status = ref('')

async function load() {
  loading.value = true
  try {
    const data = await listMyOrders({
      status: status.value || undefined,
      page: page.value,
      size: 10,
    })
    orders.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
}

watch([status, page], load)
onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>我的訂單</h3>
      <el-select v-model="status" placeholder="全部狀態" clearable size="small" style="width: 140px">
        <el-option
          v-for="(label, key) in ORDER_STATUS_LABELS"
          :key="key"
          :label="label"
          :value="key"
        />
      </el-select>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="尚無訂單" />

      <div v-for="order in orders" :key="order.id" class="order-card" @click="router.push(`/orders/${order.id}`)">
        <div class="order-top">
          <span class="order-no">{{ order.orderNo }}</span>
          <el-tag :type="ORDER_STATUS_TAG_TYPES[order.status]">{{ ORDER_STATUS_LABELS[order.status] }}</el-tag>
        </div>
        <div class="order-items">
          <span v-for="item in order.items" :key="item.id">{{ item.productName }} x{{ item.quantity }}</span>
        </div>
        <div class="order-bottom">
          <span class="date">{{ order.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
          <span class="amount">NT$ {{ order.totalAmount }}</span>
        </div>
      </div>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="10"
        :current-page="page + 1"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
}

.order-card:hover {
  border-color: #e4393c;
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.order-no {
  font-size: 13px;
  color: #999;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  margin-bottom: 10px;
}

.order-bottom {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
}

.amount {
  color: #e4393c;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
