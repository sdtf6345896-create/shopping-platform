import { describe, it, expect } from 'vitest'
import { categoryPath, flattenCategories } from './categoryTree'

describe('flattenCategories', () => {
  it('returns an empty array for an empty tree', () => {
    expect(flattenCategories([])).toEqual([])
  })

  it('flattens a single-level tree with depth 0', () => {
    const tree = [{ id: 1, name: '男裝', children: [] }]

    expect(flattenCategories(tree)).toEqual([{ id: 1, name: '男裝', depth: 0 }])
  })

  it('flattens nested children in depth-first order with increasing depth', () => {
    const tree = [
      {
        id: 1,
        name: '男裝',
        children: [
          { id: 2, name: '上衣', children: [{ id: 3, name: 'T恤', children: [] }] },
          { id: 4, name: '褲子', children: [] },
        ],
      },
      { id: 5, name: '女裝', children: [] },
    ]

    expect(flattenCategories(tree)).toEqual([
      { id: 1, name: '男裝', depth: 0 },
      { id: 2, name: '上衣', depth: 1 },
      { id: 3, name: 'T恤', depth: 2 },
      { id: 4, name: '褲子', depth: 1 },
      { id: 5, name: '女裝', depth: 0 },
    ])
  })

  it('treats a missing children field as a leaf', () => {
    const tree = [{ id: 1, name: '男裝' }]

    expect(flattenCategories(tree)).toEqual([{ id: 1, name: '男裝', depth: 0 }])
  })
})

describe('categoryPath', () => {
  const tree = [
    { id: 1, name: '女裝', children: [{ id: 11, name: '上衣', children: [] }] },
    { id: 2, name: '3C', children: [] },
  ]

  it('returns the ancestors down to the category', () => {
    expect(categoryPath(tree, 11)).toEqual([1, 11])
    expect(categoryPath(tree, 2)).toEqual([2])
  })

  it('returns an empty path for unknown categories', () => {
    expect(categoryPath(tree, 99)).toEqual([])
    expect(categoryPath(null, 1)).toEqual([])
  })
})
