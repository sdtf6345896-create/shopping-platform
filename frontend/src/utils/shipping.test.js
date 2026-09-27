import { describe, expect, it } from 'vitest'
import { amountToFreeShipping, shippingFeeFor } from './shipping'

const policy = { fee: 60, freeThreshold: 999 }

describe('shippingFeeFor', () => {
  it('charges the flat fee below the threshold', () => {
    expect(shippingFeeFor(998, policy)).toBe(60)
  })

  it('is free at or above the threshold', () => {
    expect(shippingFeeFor(999, policy)).toBe(0)
    expect(shippingFeeFor(5000, policy)).toBe(0)
  })

  it('charges nothing for an empty selection or unknown policy', () => {
    expect(shippingFeeFor(0, policy)).toBe(0)
    expect(shippingFeeFor(500, null)).toBe(0)
  })
})

describe('amountToFreeShipping', () => {
  it('returns how much more is needed', () => {
    expect(amountToFreeShipping(899, policy)).toBe(100)
    expect(amountToFreeShipping(1200, policy)).toBe(0)
  })
})
