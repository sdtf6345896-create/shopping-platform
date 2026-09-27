import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as notificationApi from '../api/notification'

// 只存未讀數量給 NavBar 的小紅點用;通知列表由通知中心頁自己載入
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refreshUnread() {
    try {
      const data = await notificationApi.getUnreadCount()
      unreadCount.value = data.count
    } catch {
      // 未登入或網路問題時不影響頁面
    }
  }

  async function markRead(id) {
    await notificationApi.markNotificationRead(id)
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  async function markAllRead() {
    await notificationApi.markAllNotificationsRead()
    unreadCount.value = 0
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, refreshUnread, markRead, markAllRead, reset }
})
