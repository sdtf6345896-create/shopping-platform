<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveReturn, listReturns, rejectReturn } from '../../api/admin/returns'
import { RETURN_STATUS_LABELS, RETURN_STATUS_TAG_TYPES } from '../../utils/orderEnums'

const router = useRouter()
const returns = ref([])
const total = ref(0)
const loading = ref(true)
const filters = reactive({ tab: 'PENDING', page: 0 })

async function load() {
  loading.value = true
  try {
    const data = await listReturns({
      status: filters.tab === 'ALL' ? undefined : filters.tab,
      page: filters.page,
      size: 10,
    })
    returns.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

async function decide(row, approve) {
  let note
  try {
    const { value } = await ElMessageBox.prompt(
      approve
        ? `核准後訂單 ${row.orderNo} 會退款(NT$ ${row.orderTotalAmount}),商品回庫存並處理購物金。處理說明(選填):`
        : `拒絕訂單 ${row.orderNo} 的退貨申請,說明會寄給會員:`,
      approve ? '核准退貨' : '拒絕退貨',
      {
        type: approve ? 'warning' : 'info',
        confirmButtonText: approve ? '核准並退款' : '拒絕',
        inputPlaceholder: approve ? '例如:已收到退回商品' : '例如:商品已拆封使用',
        inputValidator: (v) => !v || v.length <= 255 || '最多 255 字',
      },
    )
    note = value
  } catch {
    return
  }
  if (approve) {
    await approveReturn(row.id, note)
    ElMessage.success('已核准退貨並退款')
  } else {
    await rejectReturn(row.id, note)
    ElMessage.success('已拒絕退貨申請')
  }
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <h3>退貨管理</h3>

    <el-tabs v-model="filters.tab" @tab-change="handleTabChange">
      <el-tab-pane label="待審核" name="PENDING" />
      <el-tab-pane label="已核准" name="APPROVED" />
      <el-tab-pane label="已拒絕" name="REJECTED" />
      <el-tab-pane label="全部" name="ALL" />
    </el-tabs>

    <el-table v-loading="loading" :data="returns">
      <el-table-column label="訂單編號" min-width="170">
        <template #default="{ row }">
          <el-link
            type="primary"
            :underline="false"
            @click="router.push({ name: 'AdminOrderDetail', params: { id: row.orderId } })"
          >
            {{ row.orderNo }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="會員" min-width="180">
        <template #default="{ row }">
          <div>{{ row.receiverName }}</div>
          <div class="sub">{{ row.memberEmail }}</div>
        </template>
      </el-table-column>
      <el-table-column label="金額" width="110">
        <template #default="{ row }">NT$ {{ row.orderTotalAmount }}</template>
      </el-table-column>
      <el-table-column label="退貨原因" min-width="200">
        <template #default="{ row }">
          <div class="reason">{{ row.reason }}</div>
          <div v-if="row.adminNote" class="sub">處理說明:{{ row.adminNote }}</div>
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="110">
        <template #default="{ row }">
          <el-tag :type="RETURN_STATUS_TAG_TYPES[row.status]">{{ RETURN_STATUS_LABELS[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申請時間" width="150">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link size="small" type="primary" @click="decide(row, true)">核准</el-button>
            <el-button link size="small" type="danger" @click="decide(row, false)">拒絕</el-button>
          </template>
          <span v-else class="sub">{{ row.processedAt?.slice(0, 16).replace('T', ' ') }}</span>
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
.sub {
  color: #999;
  font-size: 12px;
}

.reason {
  white-space: pre-wrap;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
