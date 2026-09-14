import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import * as cartApi from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const totalQuantity = ref(0)
  const totalAmount = ref(0)
  const checkoutSelection = ref(null)

  const itemCount = computed(() => totalQuantity.value)

  async function fetchCart() {
    const data = await cartApi.getCart()
    items.value = data.items
    totalQuantity.value = data.totalQuantity
    totalAmount.value = data.totalAmount
    return data
  }

  async function addItem(skuId, quantity) {
    await cartApi.addCartItem({ skuId, quantity })
    await fetchCart()
  }

  async function updateQuantity(itemId, quantity) {
    await cartApi.updateCartItem(itemId, quantity)
    await fetchCart()
  }

  async function removeItem(itemId) {
    await cartApi.removeCartItem(itemId)
    await fetchCart()
  }

  async function clear() {
    await cartApi.clearCart()
    items.value = []
    totalQuantity.value = 0
    totalAmount.value = 0
  }

  function reset() {
    items.value = []
    totalQuantity.value = 0
    totalAmount.value = 0
    checkoutSelection.value = null
  }

  function setCheckoutSelection(ids) {
    checkoutSelection.value = ids
  }

  return {
    items,
    totalQuantity,
    totalAmount,
    checkoutSelection,
    itemCount,
    fetchCart,
    addItem,
    updateQuantity,
    removeItem,
    clear,
    reset,
    setCheckoutSelection,
  }
})
