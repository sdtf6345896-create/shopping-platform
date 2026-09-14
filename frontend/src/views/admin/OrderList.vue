<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminOrders, updateOrderStatus } from '../../api/admin/order'
import {
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TAG_TYPES,
  ORDER_STATUS_TRANSITIONS,
  PAYMENT_METHOD_LABELS,
} from '../../utils/orderEnums'

const router = useRouter()
const orders = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  status: null,
  page: 0,
})

async function load() {
  loading.value = true
  try {
    const data = await listAdminOrders({
      status: filters.status || undefined,
      page: filters.page,
      size: 10,
    })
    orders.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

async function handleTransition(order, action) {
  if (action.status === 'CANCELLED') {
    try {
      await ElMessageBox.confirm(`確定要取消訂單「${order.orderNo}」嗎?`, '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  await updateOrderStatus(order.id, action.status)
  ElMessage.success('狀態更新成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>訂單管理</h3>
      <el-select v-model="filters.status" placeholder="全部狀態" clearable style="width: 140px" @change="handleFilterChange">
        <el-option v-for="(label, key) in ORDER_STATUS_LABELS" :key="key" :label="label" :value="key" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="orders" class="table">
      <el-table-column prop="orderNo" label="訂單編號" min-width="180" />
      <el-table-column prop="receiverName" label="收件人" width="100" />
      <el-table-column label="付款方式" width="100">
        <template #default="{ row }">{{ PAYMENT_METHOD_LABELS[row.paymentMethod] }}</template>
      </el-table-column>
      <el-table-column label="金額" width="100">
        <template #default="{ row }">NT$ {{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="ORDER_STATUS_TAG_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建立時間" width="160">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link size="small" @click="router.push({ name: 'AdminOrderDetail', params: { id: row.id } })">
            查看
          </el-button>
          <el-button
            v-for="action in ORDER_STATUS_TRANSITIONS[row.status]"
            :key="action.status"
            link
            size="small"
            :type="action.type"
            @click="handleTransition(row, action)"
          >
            {{ action.label }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table {
  background: #fff;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
