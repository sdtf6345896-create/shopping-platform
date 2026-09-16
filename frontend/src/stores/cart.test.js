import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as cartApi from '../api/cart'
import { useCartStore } from './cart'

vi.mock('../api/cart')

describe('useCartStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchCart populates items, totalQuantity and totalAmount', async () => {
    cartApi.getCart.mockResolvedValue({
      items: [{ id: 1, skuId: 1, quantity: 2, subtotal: 1180 }],
      totalQuantity: 2,
      totalAmount: 1180,
    })

    const store = useCartStore()
    await store.fetchCart()

    expect(store.items).toHaveLength(1)
    expect(store.totalQuantity).toBe(2)
    expect(store.totalAmount).toBe(1180)
    expect(store.itemCount).toBe(2)
  })

  it('addItem calls the API then refreshes the cart', async () => {
    cartApi.addCartItem.mockResolvedValue()
    cartApi.getCart.mockResolvedValue({ items: [], totalQuantity: 1, totalAmount: 590 })

    const store = useCartStore()
    await store.addItem(1, 1)

    expect(cartApi.addCartItem).toHaveBeenCalledWith({ skuId: 1, quantity: 1 })
    expect(cartApi.getCart).toHaveBeenCalled()
    expect(store.totalQuantity).toBe(1)
  })

  it('clear empties the cart locally without waiting for a refetch', async () => {
    cartApi.clearCart.mockResolvedValue()

    const store = useCartStore()
    store.items = [{ id: 1 }]
    store.totalQuantity = 2
    store.totalAmount = 1180

    await store.clear()

    expect(store.items).toEqual([])
    expect(store.totalQuantity).toBe(0)
    expect(store.totalAmount).toBe(0)
    expect(cartApi.getCart).not.toHaveBeenCalled()
  })

  it('reset clears cart state and the checkout selection', () => {
    const store = useCartStore()
    store.items = [{ id: 1 }]
    store.totalQuantity = 2
    store.totalAmount = 1180
    store.setCheckoutSelection([1, 2])

    store.reset()

    expect(store.items).toEqual([])
    expect(store.totalQuantity).toBe(0)
    expect(store.totalAmount).toBe(0)
    expect(store.checkoutSelection).toBeNull()
  })
})
