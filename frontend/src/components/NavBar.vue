<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingCart, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
)

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  router.push({ path: '/products', query: keyword ? { keyword } : {} })
}

async function refreshCart() {
  if (authStore.isLoggedIn) {
    try {
      await cartStore.fetchCart()
    } catch {
      // 未登入或逾期時忽略,交由路由守衛 / 攔截器處理
    }
  } else {
    cartStore.reset()
  }
}

onMounted(refreshCart)
watch(() => authStore.isLoggedIn, refreshCart)

function handleLogout() {
  authStore.logout()
  cartStore.reset()
  router.push('/login')
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <router-link to="/" class="brand">MomoShop</router-link>

      <nav class="nav-links">
        <router-link to="/">首頁</router-link>
        <router-link to="/products">全部商品</router-link>
      </nav>

      <el-input
        v-model="searchKeyword"
        placeholder="搜尋商品"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #suffix>
          <el-icon class="search-icon" @click="handleSearch"><Search /></el-icon>
        </template>
      </el-input>

      <div class="nav-actions">
        <router-link to="/cart" class="cart-link">
          <el-badge :value="cartStore.itemCount" :hidden="cartStore.itemCount === 0">
            <el-icon :size="22"><ShoppingCart /></el-icon>
          </el-badge>
        </router-link>

        <el-dropdown v-if="authStore.isLoggedIn">
          <span class="user-entry">
            <el-icon :size="18"><User /></el-icon>
            {{ authStore.member?.name || '會員中心' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/member/profile')">個人資料</el-dropdown-item>
              <el-dropdown-item @click="router.push('/member/addresses')">收件地址</el-dropdown-item>
              <el-dropdown-item @click="router.push('/member/orders')">我的訂單</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">登出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <router-link v-else to="/login" class="login-link">登入 / 註冊</router-link>
      </div>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  font-size: 22px;
  font-weight: 700;
  color: #e4393c;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  gap: 20px;
}

.nav-links a {
  color: #333;
  font-size: 15px;
  white-space: nowrap;
}

.nav-links a.router-link-active {
  color: #e4393c;
  font-weight: 600;
}

.search-input {
  flex: 1;
  max-width: 360px;
}

.search-icon {
  cursor: pointer;
  color: #999;
}

.search-icon:hover {
  color: #e4393c;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.cart-link {
  display: flex;
  align-items: center;
  color: #333;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
}

.login-link {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}
</style>
