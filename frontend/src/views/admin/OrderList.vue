<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  downloadShipTemplate,
  exportAdminOrders,
  importShipCsv,
  listAdminOrders,
  updateOrderStatus,
} from '../../api/admin/order'
import CsvImportResultDialog from '../../components/CsvImportResultDialog.vue'
import { filenameFromDisposition, saveBlob } from '../../utils/download'
import {
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TAG_TYPES,
  ORDER_STATUS_TRANSITIONS,
  PAYMENT_METHOD_LABELS,
} from '../../utils/orderEnums'

const router = useRouter()
const route = useRoute()
const orders = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  // 從總覽頁點「待出貨」等卡片進來時會帶 ?status=
  status: route.query.status || null,
  keyword: '',
  dateRange: null, // ['YYYY-MM-DD', 'YYYY-MM-DD']
  page: 0,
})
const exporting = ref(false)

// 列表與匯出共用的查詢條件
function queryParams() {
  return {
    status: filters.status || undefined,
    keyword: filters.keyword.trim() || undefined,
    startDate: filters.dateRange?.[0],
    endDate: filters.dateRange?.[1],
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const response = await exportAdminOrders(queryParams())
    const filename = filenameFromDisposition(response.headers['content-disposition'], 'orders.csv')
    saveBlob(response.data, filename)
  } finally {
    exporting.value = false
  }
}

// 批次出貨:下載待出貨範本 → 填物流業者/單號 → 上傳(全有或全無)
const downloadingTemplate = ref(false)
const importing = ref(false)
const importResult = ref(null)

async function handleDownloadShipTemplate() {
  downloadingTemplate.value = true
  try {
    const response = await downloadShipTemplate()
    saveBlob(response.data, filenameFromDisposition(response.headers['content-disposition'], 'ship.csv'))
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleImportShip({ file }) {
  importing.value = true
  try {
    importResult.value = await importShipCsv(file)
    if (importResult.value.applied) {
      await load()
    }
  } finally {
    importing.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await listAdminOrders({
      ...queryParams(),
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
  // 出貨需要填物流資訊,統一到詳情頁處理
  if (action.status === 'SHIPPING') {
    router.push({ name: 'AdminOrderDetail', params: { id: order.id } })
    return
  }
  if (action.status === 'CANCELLED') {
    try {
      await ElMessageBox.confirm(`確定要取消訂單「${order.orderNo}」嗎?`, '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  await updateOrderStatus(order.id, { status: action.status })
  ElMessage.success('狀態更新成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>訂單管理</h3>
      <div class="header-actions">
        <el-button :loading="downloadingTemplate" @click="handleDownloadShipTemplate">下載待出貨範本</el-button>
        <el-upload :show-file-list="false" accept=".csv,text/csv" :http-request="handleImportShip">
          <el-button :loading="importing">匯入出貨單號</el-button>
        </el-upload>
        <el-button :loading="exporting" @click="handleExport">匯出 CSV</el-button>
      </div>
    </div>

    <div class="filter-row">
      <el-input
        v-model="filters.keyword"
        placeholder="訂單編號 / 收件人 / 電話 / 會員 Email"
        clearable
        style="width: 280px"
        @keyup.enter="handleFilterChange"
        @clear="handleFilterChange"
      />
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="開始日期"
        end-placeholder="結束日期"
        style="width: 260px"
        @change="handleFilterChange"
      />
      <el-select v-model="filters.status" placeholder="全部狀態" clearable style="width: 140px" @change="handleFilterChange">
        <el-option v-for="(label, key) in ORDER_STATUS_LABELS" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="handleFilterChange">搜尋</el-button>
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

    <CsvImportResultDialog
      :result="importResult"
      title="批次出貨結果"
      :success-text="`已將 ${importResult?.updated} 筆訂單改為出貨中,並已通知會員`"
      rejected-text="出貨任何訂單"
      @close="importResult = null"
    />
  </div>
</template>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
