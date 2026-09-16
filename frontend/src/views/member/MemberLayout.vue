<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menu = [
  { name: 'MemberProfile', label: '個人資料' },
  { name: 'MemberAddresses', label: '收件地址' },
  { name: 'MemberOrders', label: '我的訂單' },
  { name: 'MemberWishlist', label: '我的收藏' },
]
</script>

<template>
  <div class="page-container">
    <div class="member-layout">
      <aside class="sidebar">
        <div
          v-for="item in menu"
          :key="item.name"
          class="menu-item"
          :class="{ active: route.name === item.name }"
          @click="router.push({ name: item.name })"
        >
          {{ item.label }}
        </div>
      </aside>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.member-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.sidebar {
  width: 180px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.menu-item {
  padding: 14px 20px;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:hover {
  background: #f9f9f9;
}

.menu-item.active {
  color: #e4393c;
  font-weight: 600;
  background: #fdf0f0;
}

.content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
}
</style>
