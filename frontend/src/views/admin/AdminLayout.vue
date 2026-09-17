<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '../../stores/adminAuth'

const route = useRoute()
const router = useRouter()
const adminAuthStore = useAdminAuthStore()

const menu = [
  { name: 'AdminProductList', label: '商品管理' },
  { name: 'AdminBannerList', label: 'Banner 管理' },
  { name: 'AdminCategoryList', label: '分類管理' },
  { name: 'AdminOrderList', label: '訂單管理', matchNames: ['AdminOrderList', 'AdminOrderDetail'] },
  { name: 'AdminCouponList', label: '優惠券管理' },
  { name: 'AdminReport', label: '銷售報表' },
  { name: 'AdminMemberList', label: '會員管理' },
]

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
          v-for="item in menu"
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
        <el-button link @click="handleLogout">登出</el-button>
      </header>
      <div class="content">
        <router-view />
      </div>
    </div>
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
