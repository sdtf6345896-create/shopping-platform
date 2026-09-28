<script setup>
// 後台帳號管理(僅 ADMIN):新增管理員 / 客服、調整角色與狀態、重設密碼
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminAccount,
  listAdminAccounts,
  resetAdminPassword,
  updateAdminAccount,
} from '../../api/admin/account'
import { useAdminAuthStore } from '../../stores/adminAuth'
import { ROLE_LABELS } from '../../utils/adminPermissions'

const adminAuthStore = useAdminAuthStore()
const accounts = ref([])
const loading = ref(true)

const dialogVisible = ref(false)
const editing = ref(null) // null = 新增
const saving = ref(false)
const formRef = ref()
const form = reactive({ username: '', name: '', password: '', role: 'STAFF', status: 'ACTIVE' })
const rules = {
  username: [
    { required: true, message: '請輸入帳號', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.-]{3,50}$/, message: '3~50 碼英數字,可含 _ . -', trigger: 'blur' },
  ],
  name: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '請輸入初始密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少 8 碼', trigger: 'blur' },
  ],
}

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')
const isSelf = (row) => row.id === adminAuthStore.admin?.id

async function load() {
  loading.value = true
  try {
    accounts.value = await listAdminAccounts()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', name: '', password: '', role: 'STAFF', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, { username: row.username, name: row.name, password: '', role: row.role, status: row.status })
  dialogVisible.value = true
}

async function handleSave() {
  if (editing.value) {
    if (!form.name.trim()) {
      ElMessage.warning('請輸入姓名')
      return
    }
  } else {
    await formRef.value.validate()
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateAdminAccount(editing.value.id, { name: form.name, role: form.role, status: form.status })
    } else {
      await createAdminAccount({ username: form.username, name: form.name, password: form.password, role: form.role })
    }
    ElMessage.success('已儲存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleResetPassword(row) {
  let value
  try {
    const result = await ElMessageBox.prompt(`為 ${row.username} 設定新密碼(至少 8 碼),請再告知對方`, '重設密碼', {
      inputType: 'password',
      inputValidator: (v) => (v || '').length >= 8 || '密碼長度至少 8 碼',
      confirmButtonText: '重設',
    })
    value = result.value
  } catch {
    return
  }
  await resetAdminPassword(row.id, value)
  ElMessage.success('密碼已重設')
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>帳號管理</h3>
      <el-button type="primary" @click="openCreate">新增帳號</el-button>
    </div>
    <p class="hint">
      「客服」可處理訂單、退貨、商品問答、評價與訂單留言,商品與會員只能查看;其他功能僅「管理員」可用。停用或調整角色會立即生效。
    </p>

    <el-table v-loading="loading" :data="accounts">
      <el-table-column prop="username" label="帳號" width="160" />
      <el-table-column label="姓名" width="160">
        <template #default="{ row }">
          {{ row.name }}
          <el-tag v-if="isSelf(row)" size="small" effect="plain">我</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ ROLE_LABELS[row.role] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '啟用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建立時間" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link size="small" @click="openEdit(row)">編輯</el-button>
          <el-button link size="small" @click="handleResetPassword(row)">重設密碼</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '編輯帳號' : '新增帳號'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="帳號" prop="username">
          <el-input v-model="form.username" :disabled="!!editing" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密碼" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role" :disabled="editing && isSelf(editing)">
            <el-radio value="STAFF">客服</el-radio>
            <el-radio value="ADMIN">管理員</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editing" label="狀態">
          <el-radio-group v-model="form.status" :disabled="isSelf(editing)">
            <el-radio value="ACTIVE">啟用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <p v-if="editing && isSelf(editing)" class="hint">不能調整自己的角色或狀態,請由其他管理員操作。</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  color: #999;
  font-size: 12px;
}
</style>
