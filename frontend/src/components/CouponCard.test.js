import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CouponCard from './CouponCard.vue'

describe('CouponCard', () => {
  it('shows discount, threshold, code and expiry', () => {
    const wrapper = mount(CouponCard, {
      props: {
        coupon: {
          code: 'SAVE100', name: '滿千折百', discountType: 'FIXED_AMOUNT', discountValue: 100,
          minSpendAmount: 1000, endAt: '2026-12-31T23:59:59',
        },
      },
      slots: { default: '<button class="claim">領取</button>' },
    })

    expect(wrapper.text()).toContain('折抵 NT$ 100')
    expect(wrapper.text()).toContain('滿 NT$ 1000 可用')
    expect(wrapper.text()).toContain('SAVE100')
    expect(wrapper.text()).toContain('2026-12-31')
    expect(wrapper.find('.claim').exists()).toBe(true)
  })

  it('labels coupons without threshold or expiry', () => {
    const wrapper = mount(CouponCard, {
      props: { coupon: { code: 'FREE', name: '全館', discountType: 'PERCENTAGE', discountValue: 10, minSpendAmount: 0 } },
    })

    expect(wrapper.text()).toContain('無門檻')
    expect(wrapper.text()).toContain('無使用期限')
  })
})
