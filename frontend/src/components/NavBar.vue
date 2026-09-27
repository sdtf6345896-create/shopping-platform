<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Search, ShoppingCart, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { suggestProducts } from '../api/product'
import { useNotificationStore } from '../stores/notification'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()
const notificationStore = useNotificationStore()

const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
)

// 搜尋建議:輸入時列出相符的熱銷商品,點選直接進商品頁;按 Enter 仍是一般搜尋
async function fetchSuggestions(query, callback) {
  if (!query?.trim()) {
    callback([])
    return
  }
  try {
    callback(await suggestProducts(query.trim()))
  } catch {
    callback([])
  }
}

function handleSelectSuggestion(product) {
  router.push(`/products/${product.id}`)
}

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

function refreshNotifications() {
  if (authStore.isLoggedIn) {
    notificationStore.refreshUnread()
  } else {
    notificationStore.reset()
  }
}

onMounted(() => {
  refreshCart()
  refreshNotifications()
})
watch(
  () => authStore.isLoggedIn,
  () => {
    refreshCart()
    refreshNotifications()
  },
)
// 換頁時順便更新未讀數(例如剛付款、訂單狀態改變後)
watch(() => route.path, refreshNotifications)

function handleLogout() {
  authStore.logout()
  cartStore.reset()
  notificationStore.reset()
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

      <el-autocomplete
        v-model="searchKeyword"
        placeholder="搜尋商品"
        class="search-input"
        clearable
        value-key="name"
        :fetch-suggestions="fetchSuggestions"
        :trigger-on-focus="false"
        :debounce="250"
        @select="handleSelectSuggestion"
        @keyup.enter="handleSearch"
      >
        <template #default="{ item }">
          <div class="suggestion">
            <span class="suggestion-name">{{ item.name }}</span>
            <span class="suggestion-price">NT$ {{ item.salePrice ?? item.price }}</span>
          </div>
        </template>
        <template #suffix>
          <el-icon class="search-icon" @click="handleSearch"><Search /></el-icon>
        </template>
      </el-autocomplete>

      <div class="nav-actions">
        <router-link v-if="authStore.isLoggedIn" to="/member/notifications" class="cart-link" title="通知中心">
          <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount === 0">
            <el-icon :size="22"><Bell /></el-icon>
          </el-badge>
        </router-link>

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
              <el-dropdown-item @click="router.push('/member/wishlist')">我的收藏</el-dropdown-item>
              <el-dropdown-item @click="router.push('/member/points')">我的購物金</el-dropdown-item>
              <el-dropdown-item @click="router.push('/member/notifications')">通知中心</el-dropdown-item>
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
.suggestion {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.suggestion-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.suggestion-price {
  color: #e4393c;
  white-space: nowrap;
}

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
