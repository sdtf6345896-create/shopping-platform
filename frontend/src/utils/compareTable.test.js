import { describe, expect, it } from 'vitest'
import { buildSpecRows } from './compareTable'

describe('buildSpecRows', () => {
  it('unions spec names in first-seen order and fills gaps', () => {
    const rows = buildSpecRows([
      { specs: [{ name: '材質', value: '純棉' }, { name: '產地', value: '台灣' }] },
      { specs: [{ name: '產地', value: '台灣' }, { name: '重量', value: '200g' }] },
      { specs: [] },
    ])

    expect(rows.map((r) => r.name)).toEqual(['材質', '產地', '重量'])
    expect(rows[0].values).toEqual(['純棉', '-', '-'])
    expect(rows[1].values).toEqual(['台灣', '台灣', '-'])
    expect(rows[1].same).toBe(false)
  })

  it('marks rows where every product matches', () => {
    const rows = buildSpecRows([{ specs: [{ name: '產地', value: '台灣' }] }, { specs: [{ name: '產地', value: '台灣' }] }])
    expect(rows[0].same).toBe(true)
  })
})
