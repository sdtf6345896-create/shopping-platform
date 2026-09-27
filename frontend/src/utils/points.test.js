import { describe, expect, it } from 'vitest'
import { maxRedeemable } from './points'

describe('maxRedeemable', () => {
  it('caps at half of the payable amount, rounded down', () => {
    expect(maxRedeemable(1000, 1181, 0.5)).toBe(590)
  })

  it('caps at the balance', () => {
    expect(maxRedeemable(100, 1181, 0.5)).toBe(100)
  })

  it('is zero without balance or payable amount', () => {
    expect(maxRedeemable(0, 1000, 0.5)).toBe(0)
    expect(maxRedeemable(100, 0, 0.5)).toBe(0)
    expect(maxRedeemable(100, -50, 0.5)).toBe(0)
  })
})
