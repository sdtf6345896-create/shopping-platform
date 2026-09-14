<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminMembers, updateMemberStatus } from '../../api/admin/member'

const members = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  status: null,
  keyword: '',
  page: 0,
})

async function load() {
  loading.value = true
  try {
    const data = await listAdminMembers({
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      page: filters.page,
      size: 10,
    })
    members.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

async function handleToggleStatus(row) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (next === 'DISABLED') {
    try {
      await ElMessageBox.confirm(`確定要停用會員「${row.name}」嗎?停用後該會員將無法登入。`, '提示', {
        type: 'warning',
      })
    } catch {
      return
    }
  }
  await updateMemberStatus(row.id, next)
  ElMessage.success(next === 'ACTIVE' ? '已啟用' : '已停用')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>會員管理</h3>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="全部狀態" clearable style="width: 140px" @change="handleSearch">
        <el-option label="啟用中" value="ACTIVE" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜尋姓名或 Email"
        style="width: 240px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button @click="handleSearch">搜尋</el-button>
    </div>

    <el-table v-loading="loading" :data="members" class="table">
      <el-table-column prop="email" label="Email" min-width="200" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手機" width="130" />
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '啟用中' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="註冊時間" width="160">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link size="small" :type="row.status === 'ACTIVE' ? 'danger' : 'primary'" @click="handleToggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '啟用' }}
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

.filter-bar {
  display: flex;
  gap: 12px;
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
