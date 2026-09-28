import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PrintableOrder from './PrintableOrder.vue'

const order = {
  orderNo: 'ORD001', createdAt: '2026-09-27T10:00:00', paymentMethod: 'CREDIT_CARD',
  receiverName: '王小明', receiverPhone: '0912345678', receiverAddress: '台北市大安區',
  subtotalAmount: 1180, discountAmount: 100, couponCode: 'SAVE100', pointsUsed: 0, shippingFee: 0, totalAmount: 1080,
  items: [
    { id: 1, skuCode: 'TSHIRT-BLK-M', productName: '經典圓領T恤', specName: '黑色/M', unitPrice: 590, quantity: 2, subtotal: 1180 },
  ],
}

describe('PrintableOrder', () => {
  it('receipt shows prices and totals but not SKU codes', () => {
    const wrapper = mount(PrintableOrder, { props: { order, variant: 'receipt' } })

    expect(wrapper.text()).toContain('購物收據')
    expect(wrapper.text()).toContain('實付金額')
    expect(wrapper.text()).toContain('NT$ 1080')
    expect(wrapper.text()).not.toContain('TSHIRT-BLK-M')
  })

  it('packing slip shows SKU codes and quantities but no prices', () => {
    const wrapper = mount(PrintableOrder, { props: { order, variant: 'packing' } })

    expect(wrapper.text()).toContain('揀貨單')
    expect(wrapper.text()).toContain('TSHIRT-BLK-M')
    expect(wrapper.text()).toContain('2 件')
    expect(wrapper.text()).not.toContain('NT$')
  })

  it('packing slip flags gift orders and prints the card message, still without prices', () => {
    const gift = { ...order, giftWrap: true, giftWrapFee: 30, giftMessage: '生日快樂!' }
    const packing = mount(PrintableOrder, { props: { order: gift, variant: 'packing' } })

    expect(packing.text()).toContain('禮品包裝')
    expect(packing.text()).toContain('賀卡:生日快樂!')
    expect(packing.text()).not.toContain('NT$')

    const receipt = mount(PrintableOrder, { props: { order: gift, variant: 'receipt' } })
    expect(receipt.text()).toContain('NT$ 30')
  })
})
