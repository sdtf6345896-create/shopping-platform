import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const MAX_COMPARE = 4
const STORAGE_KEY = 'compare.productIds'

function readIds() {
  try {
    const ids = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(ids) ? ids.filter((id) => Number.isInteger(id)).slice(0, MAX_COMPARE) : []
  } catch {
    return []
  }
}

// 商品比較清單(最多 4 件),存在瀏覽器,不需登入
export const useCompareStore = defineStore('compare', () => {
  const ids = ref(readIds())
  const count = computed(() => ids.value.length)
  const isFull = computed(() => ids.value.length >= MAX_COMPARE)

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.value))
    } catch {
      // 無痕模式等情況存不了,只影響重新整理後的保留
    }
  }

  function has(id) {
    return ids.value.includes(Number(id))
  }

  /** @returns 是否加入成功(已滿時回傳 false) */
  function add(id) {
    const productId = Number(id)
    if (has(productId)) return true
    if (isFull.value) return false
    ids.value = [...ids.value, productId]
    persist()
    return true
  }

  function remove(id) {
    ids.value = ids.value.filter((x) => x !== Number(id))
    persist()
  }

  function clear() {
    ids.value = []
    persist()
  }

  return { ids, count, isFull, has, add, remove, clear }
})
