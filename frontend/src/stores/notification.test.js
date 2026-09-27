import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as notificationApi from '../api/notification'
import { useNotificationStore } from './notification'

vi.mock('../api/notification')

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('refreshUnread loads the unread count', async () => {
    notificationApi.getUnreadCount.mockResolvedValue({ count: 3 })
    const store = useNotificationStore()

    await store.refreshUnread()

    expect(store.unreadCount).toBe(3)
  })

  it('refreshUnread keeps the old count when the request fails', async () => {
    notificationApi.getUnreadCount.mockRejectedValue(new Error('401'))
    const store = useNotificationStore()
    store.unreadCount = 2

    await store.refreshUnread()

    expect(store.unreadCount).toBe(2)
  })

  it('markRead decrements without going negative', async () => {
    notificationApi.markNotificationRead.mockResolvedValue({})
    const store = useNotificationStore()
    store.unreadCount = 1

    await store.markRead(5)
    await store.markRead(6)

    expect(notificationApi.markNotificationRead).toHaveBeenCalledWith(5)
    expect(store.unreadCount).toBe(0)
  })

  it('markAllRead clears the count', async () => {
    notificationApi.markAllNotificationsRead.mockResolvedValue({ updated: 4 })
    const store = useNotificationStore()
    store.unreadCount = 4

    await store.markAllRead()

    expect(store.unreadCount).toBe(0)
  })
})
