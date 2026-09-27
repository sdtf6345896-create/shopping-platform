import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import * as memberApi from '../api/member'
import MemberTierCard from './MemberTierCard.vue'

vi.mock('../api/member')

describe('MemberTierCard', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows the tier, multiplier and how much more to spend for the next tier', async () => {
    memberApi.getMyTier.mockResolvedValue({
      tier: 'SILVER', label: '銀卡會員', pointsMultiplier: 1.5, spending: 8000,
      nextTier: 'GOLD', nextLabel: '金卡會員', amountToNext: 12000,
    })
    const wrapper = mount(MemberTierCard)
    await flushPromises()

    expect(wrapper.find('.tier-badge').text()).toBe('銀卡會員')
    expect(wrapper.text()).toContain('回饋 1.5 倍')
    expect(wrapper.text()).toContain('再消費 NT$ 12,000 升級為金卡會員')
  })

  it('congratulates members already at the top tier', async () => {
    memberApi.getMyTier.mockResolvedValue({
      tier: 'GOLD', label: '金卡會員', pointsMultiplier: 2, spending: 30000,
      nextTier: null, nextLabel: null, amountToNext: 0,
    })
    const wrapper = mount(MemberTierCard)
    await flushPromises()

    expect(wrapper.text()).toContain('已是最高等級')
  })
})
