<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adjustMemberPoints, listAdminMembers, updateMemberStatus } from '../../api/admin/member'

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

const pointsDialog = reactive({ visible: false, member: null, amount: 100, reason: '', saving: false })

function openPointsDialog(row) {
  Object.assign(pointsDialog, { visible: true, member: row, amount: 100, reason: '' })
}

async function submitPoints() {
  if (!pointsDialog.amount) {
    ElMessage.warning('調整點數不可為 0')
    return
  }
  if (!pointsDialog.reason.trim()) {
    ElMessage.warning('請輸入調整原因')
    return
  }
  pointsDialog.saving = true
  try {
    const result = await adjustMemberPoints(pointsDialog.member.id, pointsDialog.amount, pointsDialog.reason)
    ElMessage.success(`已調整,目前餘額 ${result.balance} 點`)
    pointsDialog.visible = false
    await load()
  } finally {
    pointsDialog.saving = false
  }
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
      <el-table-column label="購物金" width="90" align="right">
        <template #default="{ row }">{{ row.points }}</template>
      </el-table-column>
      <el-table-column label="註冊時間" width="160">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="openPointsDialog(row)">調整購物金</el-button>
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

    <el-dialog v-model="pointsDialog.visible" title="調整購物金" width="420px">
      <template v-if="pointsDialog.member">
        <p class="dialog-member">
          {{ pointsDialog.member.name }}({{ pointsDialog.member.email }})目前 {{ pointsDialog.member.points }} 點
        </p>
        <el-form label-width="80px" @submit.prevent>
          <el-form-item label="調整點數">
            <el-input-number v-model="pointsDialog.amount" :min="-100000" :max="100000" :step="50" />
            <span class="hint">負數為扣除</span>
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="pointsDialog.reason" maxlength="100" placeholder="例如:客服補償、活動贈點" />
          </el-form-item>
        </el-form>
        <p class="hint">會員會收到站內通知,此操作會記錄在操作紀錄中。</p>
      </template>
      <template #footer>
        <el-button @click="pointsDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pointsDialog.saving" @click="submitPoints">確認調整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialog-member {
  margin: 0 0 16px;
  color: #333;
}

.hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}

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
