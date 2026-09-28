import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { MAX_COMPARE, useCompareStore } from './compare'

describe('compare store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('adds up to the limit without duplicates and persists', () => {
    const store = useCompareStore()
    expect(store.add(1)).toBe(true)
    expect(store.add('1')).toBe(true)
    for (let id = 2; id <= MAX_COMPARE; id++) store.add(id)
    expect(store.add(99)).toBe(false)
    expect(store.ids).toEqual([1, 2, 3, 4])
    expect(JSON.parse(localStorage.getItem('compare.productIds'))).toEqual([1, 2, 3, 4])
  })

  it('restores from storage, ignoring junk', () => {
    localStorage.setItem('compare.productIds', JSON.stringify([5, 'x', 6]))
    setActivePinia(createPinia())
    const store = useCompareStore()
    expect(store.ids).toEqual([5, 6])
    store.remove(5)
    expect(store.has(5)).toBe(false)
    store.clear()
    expect(store.count).toBe(0)
  })
})
