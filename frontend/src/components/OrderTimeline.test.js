import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import OrderTimeline from './OrderTimeline.vue'

const logs = [
  { fromStatus: null, toStatus: 'PENDING_PAYMENT', actor: 'MEMBER', note: '訂單成立', createdAt: '2026-09-27T10:00:00' },
  { fromStatus: 'PENDING_PAYMENT', toStatus: 'PAID', actor: 'MEMBER', note: null, createdAt: '2026-09-27T10:05:00' },
  { fromStatus: 'PAID', toStatus: 'SHIPPING', actor: 'ADMIN', note: '黑貓 TRK1', createdAt: '2026-09-28T09:00:00' },
]

describe('OrderTimeline', () => {
  it('lists the newest status first with its note and timestamp', () => {
    const wrapper = mount(OrderTimeline, { props: { logs } })

    const statuses = wrapper.findAll('.status').map((s) => s.text())
    expect(statuses).toEqual(['出貨中', '已付款', '待付款'])
    expect(wrapper.text()).toContain('黑貓 TRK1')
    expect(wrapper.text()).toContain('2026-09-28 09:00:00')
  })

  it('hides who made the change unless asked to show it', () => {
    expect(mount(OrderTimeline, { props: { logs } }).find('.actor').exists()).toBe(false)

    const withActor = mount(OrderTimeline, { props: { logs, showActor: true } })
    expect(withActor.findAll('.actor').map((a) => a.text())).toEqual(['(管理員)', '(會員)', '(會員)'])
  })

  it('shows an empty state without logs', () => {
    expect(mount(OrderTimeline, { props: { logs: [] } }).text()).toContain('尚無狀態紀錄')
  })
})
