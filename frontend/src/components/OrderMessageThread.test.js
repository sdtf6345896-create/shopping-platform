import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import * as orderApi from '../api/order'
import * as adminOrderApi from '../api/admin/order'
import OrderMessageThread from './OrderMessageThread.vue'

vi.mock('../api/order')
vi.mock('../api/admin/order')

const thread = [
  { id: 1, sender: 'MEMBER', content: '可以週六送嗎?', createdAt: '2026-09-28T10:00:00' },
  { id: 2, sender: 'ADMIN', content: '可以的', createdAt: '2026-09-28T11:30:00' },
]

describe('OrderMessageThread', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    orderApi.getOrderMessages.mockResolvedValue([...thread])
    adminOrderApi.listAdminOrderMessages.mockResolvedValue([...thread])
  })

  it('labels the member side as 我 and the seller as 賣家 for members', async () => {
    const wrapper = mount(OrderMessageThread, { props: { orderId: 7 } })
    await flushPromises()

    expect(orderApi.getOrderMessages).toHaveBeenCalledWith(7)
    const metas = wrapper.findAll('.meta').map((m) => m.text())
    expect(metas[0]).toContain('我')
    expect(metas[1]).toContain('賣家')
    expect(wrapper.findAll('.message.mine')).toHaveLength(1)
  })

  it('uses the admin API and labels the buyer side in admin mode', async () => {
    adminOrderApi.replyOrderMessage.mockResolvedValue({
      id: 3, sender: 'ADMIN', content: '已安排', createdAt: '2026-09-28T12:00:00',
    })
    const wrapper = mount(OrderMessageThread, { props: { orderId: 7, mode: 'admin' } })
    await flushPromises()

    expect(wrapper.findAll('.meta')[0].text()).toContain('買家')
    await wrapper.find('textarea').setValue('  已安排  ')
    await wrapper.find('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(adminOrderApi.replyOrderMessage).toHaveBeenCalledWith(7, '已安排')
    expect(orderApi.postOrderMessage).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('已安排')
    expect(wrapper.find('textarea').element.value).toBe('')
  })
})
