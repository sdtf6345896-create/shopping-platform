<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminAuthStore } from '../../stores/adminAuth'
import { changeMyAdminPassword } from '../../api/admin/account'
import { ROLE_LABELS, canAccessAdminRoute } from '../../utils/adminPermissions'

const route = useRoute()
const router = useRouter()
const adminAuthStore = useAdminAuthStore()

const menu = [
  { name: 'AdminDashboard', label: '總覽' },
  { name: 'AdminProductList', label: '商品管理' },
  { name: 'AdminBannerList', label: 'Banner 管理' },
  { name: 'AdminCategoryList', label: '分類管理' },
  { name: 'AdminOrderList', label: '訂單管理', matchNames: ['AdminOrderList', 'AdminOrderDetail'] },
  { name: 'AdminReturnList', label: '退貨管理' },
  { name: 'AdminPromotionList', label: '滿件活動' },
  { name: 'AdminCouponList', label: '優惠券管理' },
  { name: 'AdminQuestionList', label: '商品問答' },
  { name: 'AdminReviewList', label: '評價管理' },
  { name: 'AdminOrderMessageList', label: '訂單留言' },
  { name: 'AdminReport', label: '銷售報表' },
  { name: 'AdminMemberList', label: '會員管理', matchNames: ['AdminMemberList', 'AdminMemberDetail'] },
  { name: 'AdminAccountList', label: '帳號管理' },
  { name: 'AdminAuditLogList', label: '操作紀錄' },
]

// 客服只看得到自己能用的選單
const visibleMenu = computed(() => menu.filter((item) => canAccessAdminRoute(adminAuthStore.role, item.name)))

const passwordDialogVisible = ref(false)
const passwordFormRef = ref()
const changingPassword = ref(false)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  currentPassword: [{ required: true, message: '請輸入目前密碼', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '請輸入新密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少 8 碼', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) =>
        value === passwordForm.newPassword ? callback() : callback(new Error('兩次輸入的新密碼不一致')),
      trigger: 'blur',
    },
  ],
}

function openPasswordDialog() {
  Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
  passwordDialogVisible.value = true
}

async function handleChangePassword() {
  await passwordFormRef.value.validate()
  changingPassword.value = true
  try {
    await changeMyAdminPassword(passwordForm.currentPassword, passwordForm.newPassword)
    ElMessage.success('密碼已更新')
    passwordDialogVisible.value = false
  } finally {
    changingPassword.value = false
  }
}

function isActive(item) {
  const names = item.matchNames || [item.name]
  return names.includes(route.name)
}

function handleLogout() {
  adminAuthStore.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">MomoShop 後台</div>
      <nav class="menu">
        <div
          v-for="item in visibleMenu"
          :key="item.name"
          class="menu-item"
          :class="{ active: isActive(item) }"
          @click="router.push({ name: item.name })"
        >
          {{ item.label }}
        </div>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <span>{{ adminAuthStore.admin?.name || adminAuthStore.admin?.username }}</span>
        <el-tag v-if="adminAuthStore.role" size="small" :type="adminAuthStore.isAdmin ? 'danger' : 'info'">
          {{ ROLE_LABELS[adminAuthStore.role] }}
        </el-tag>
        <el-button link @click="openPasswordDialog">修改密碼</el-button>
        <el-button link @click="handleLogout">登出</el-button>
      </header>
      <div class="content">
        <router-view />
      </div>
    </div>

    <el-dialog v-model="passwordDialogVisible" title="修改密碼" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="目前密碼" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密碼" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="確認新密碼" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">更新密碼</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #1f2937;
  color: #fff;
}

.brand {
  padding: 20px;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid #374151;
}

.menu-item {
  padding: 14px 20px;
  cursor: pointer;
  font-size: 14px;
  color: #d1d5db;
}

.menu-item:hover {
  background: #374151;
}

.menu-item.active {
  background: #e4393c;
  color: #fff;
}

.main {
  flex: 1;
  min-width: 0;
  background: #f5f5f5;
}

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 0 24px;
  font-size: 14px;
}

.content {
  padding: 24px;
}
</style>
