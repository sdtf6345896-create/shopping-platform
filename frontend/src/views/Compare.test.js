import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import * as productApi from '../api/product'
import { useCompareStore } from '../stores/compare'
import Compare from './Compare.vue'

vi.mock('../api/product')
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRouter: () => ({ push: vi.fn() }),
}))

const product = (id, specs) => ({
  id, name: `商品${id}`, price: 100 * id, salePrice: null, categoryName: '分類', salesCount: 0,
  ratingAverage: 0, reviewCount: 0, mainImage: null, skus: [{ specName: '標準', stock: 3 }], specs,
})

describe('Compare', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('drops every unavailable product and keeps the rest', async () => {
    const store = useCompareStore()
    store.add(1)
    store.add(2)
    store.add(3)
    productApi.getProductDetail.mockImplementation((id) =>
      id === 1 || id === 2 ? Promise.reject(new Error('404')) : Promise.resolve(product(id, [{ name: '產地', value: `地${id}` }])),
    )

    const wrapper = mount(Compare, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await flushPromises()

    // 移除的是無法載入的 1 和 2;逐筆移除時若用變動中的清單取索引,會誤刪到 3
    expect(store.ids).toEqual([3])
    expect(wrapper.text()).toContain('商品3')
    expect(wrapper.text()).toContain('地3')
    expect(wrapper.text()).not.toContain('商品2')
  })
})
