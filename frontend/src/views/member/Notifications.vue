<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications } from '../../api/notification'
import { useNotificationStore } from '../../stores/notification'

const TYPE_LABELS = { ORDER: '訂單', RETURN: '退貨', QUESTION: '問答', SYSTEM: '系統' }
const PAGE_SIZE = 10

const router = useRouter()
const notificationStore = useNotificationStore()
const items = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const data = await listNotifications({ page: page.value, size: PAGE_SIZE })
    items.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

async function open(item) {
  if (!item.read) {
    await notificationStore.markRead(item.id)
    item.read = true
  }
  if (item.link) {
    router.push(item.link)
  }
}

async function handleMarkAll() {
  await notificationStore.markAllRead()
  items.value.forEach((item) => {
    item.read = true
  })
}

function handlePageChange(p) {
  page.value = p - 1
  load()
}

onMounted(() => {
  load()
  notificationStore.refreshUnread()
})
</script>

<template>
  <div>
    <div class="header-row">
      <h3>通知中心</h3>
      <el-button size="small" :disabled="notificationStore.unreadCount === 0" @click="handleMarkAll">
        全部標為已讀
      </el-button>
    </div>

    <div v-loading="loading" class="list">
      <el-empty v-if="!loading && items.length === 0" description="目前沒有通知" :image-size="60" />
      <div
        v-for="item in items"
        :key="item.id"
        class="item"
        :class="{ unread: !item.read, clickable: !!item.link }"
        role="button"
        tabindex="0"
        @click="open(item)"
        @keyup.enter="open(item)"
      >
        <span class="dot" />
        <div class="body">
          <div class="title-row">
            <el-tag size="small" effect="plain">{{ TYPE_LABELS[item.type] || item.type }}</el-tag>
            <span class="title">{{ item.title }}</span>
            <span class="time">{{ item.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
          </div>
          <p class="content">{{ item.content }}</p>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > PAGE_SIZE"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="PAGE_SIZE"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list {
  margin-top: 12px;
  min-height: 80px;
}

.item {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.item.clickable {
  cursor: pointer;
}

.item.clickable:hover {
  background: #fafafa;
}

.dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 8px;
  border-radius: 50%;
  background: transparent;
}

.item.unread .dot {
  background: #e4393c;
}

.body {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-weight: 600;
}

.item:not(.unread) .title {
  font-weight: normal;
  color: #666;
}

.time {
  margin-left: auto;
  color: #999;
  font-size: 12px;
  white-space: nowrap;
}

.content {
  margin: 4px 0 0;
  color: #666;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
