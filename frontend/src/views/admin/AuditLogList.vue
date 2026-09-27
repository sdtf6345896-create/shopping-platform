<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs } from '../../api/admin/auditLog'

const TARGET_LABELS = {
  PRODUCT: '商品',
  CATEGORY: '分類',
  BANNER: 'Banner',
  COUPON: '優惠券',
  ORDER: '訂單',
  RETURN: '退貨',
  MEMBER: '會員',
  QUESTION: '商品問答',
  UPLOAD: '上傳',
}

const logs = ref([])
const total = ref(0)
const loading = ref(true)
const filters = reactive({ adminUsername: '', targetType: null, targetId: '', page: 0 })

async function load() {
  loading.value = true
  try {
    const data = await listAuditLogs({
      adminUsername: filters.adminUsername.trim() || undefined,
      targetType: filters.targetType || undefined,
      targetId: filters.targetId.trim() || undefined,
      page: filters.page,
      size: 20,
    })
    logs.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h3>操作紀錄</h3>

    <div class="filter-row">
      <el-input v-model="filters.adminUsername" placeholder="管理員帳號" clearable style="width: 160px" @keyup.enter="search" />
      <el-select v-model="filters.targetType" placeholder="全部對象" clearable style="width: 140px" @change="search">
        <el-option v-for="(label, key) in TARGET_LABELS" :key="key" :label="label" :value="key" />
      </el-select>
      <el-input v-model="filters.targetId" placeholder="對象 ID" clearable style="width: 120px" @keyup.enter="search" />
      <el-button type="primary" @click="search">搜尋</el-button>
    </div>

    <el-table v-loading="loading" :data="logs" size="small">
      <el-table-column label="時間" width="160">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column prop="adminUsername" label="管理員" width="110" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column label="對象" width="140">
        <template #default="{ row }">
          {{ TARGET_LABELS[row.targetType] || row.targetType }}<span v-if="row.targetId" class="sub"> #{{ row.targetId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="內容" min-width="220">
        <template #default="{ row }">
          <span>{{ row.detail }}</span>
          <div v-if="row.errorMessage" class="error">{{ row.errorMessage }}</div>
        </template>
      </el-table-column>
      <el-table-column label="結果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失敗' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="20"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 16px;
}

.sub {
  color: #999;
}

.error {
  color: #e4393c;
  font-size: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
