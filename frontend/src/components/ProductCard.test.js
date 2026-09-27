import { describe, expect, it } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import ProductCard from './ProductCard.vue'

const mountCard = (product) =>
  mount(ProductCard, { props: { product }, global: { stubs: { RouterLink: RouterLinkStub } } })

describe('ProductCard', () => {
  it('shows the average rating and review count when reviewed', () => {
    const wrapper = mountCard({ id: 1, name: 'T恤', price: 590, ratingAverage: 4.5, reviewCount: 12 })

    expect(wrapper.find('.rating').text()).toContain('★ 4.5')
    expect(wrapper.find('.rating').text()).toContain('(12)')
    expect(wrapper.findComponent(RouterLinkStub).props('to')).toBe('/products/1')
  })

  it('hides the rating when there are no reviews yet', () => {
    const wrapper = mountCard({ id: 2, name: '水壺', price: 399, ratingAverage: 0, reviewCount: 0 })

    expect(wrapper.find('.rating').exists()).toBe(false)
  })

  it('shows the sale price, a discount badge and the struck-through list price during a flash sale', () => {
    const wrapper = mountCard({
      id: 3, name: '耳機', price: 1000, salePrice: 800, saleDiscountPercent: 20, reviewCount: 0,
    })

    expect(wrapper.find('.sale-badge').text()).toBe('-20%')
    expect(wrapper.find('.price').text()).toContain('NT$ 800')
    expect(wrapper.find('.original-price').text()).toBe('NT$ 1000')
  })

  it('shows only the list price when not on sale', () => {
    const wrapper = mountCard({ id: 4, name: '耳機', price: 1000, salePrice: null, reviewCount: 0 })

    expect(wrapper.find('.sale-badge').exists()).toBe(false)
    expect(wrapper.find('.original-price').exists()).toBe(false)
  })
})
