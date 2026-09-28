import { describe, expect, it } from 'vitest'
import { amountToFreeShipping, shippingFeeFor, validateCvsPickup } from './shipping'

const policy = { fee: 60, cvsFee: 45, freeThreshold: 999 }

describe('shippingFeeFor', () => {
  it('charges the flat fee below the threshold', () => {
    expect(shippingFeeFor(998, policy)).toBe(60)
  })

  it('charges the cheaper fee for convenience-store pickup', () => {
    expect(shippingFeeFor(998, policy, 'CVS_PICKUP')).toBe(45)
    expect(shippingFeeFor(999, policy, 'CVS_PICKUP')).toBe(0)
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

describe('validateCvsPickup', () => {
  const valid = { brand: 'SEVEN_ELEVEN', storeName: '信義門市', storeCode: '', recipientName: '王小明', recipientPhone: '0912345678' }

  it('accepts a complete pickup with or without store code', () => {
    expect(validateCvsPickup(valid)).toBeNull()
    expect(validateCvsPickup({ ...valid, storeCode: '123456' })).toBeNull()
  })

  it('reports the first problem', () => {
    expect(validateCvsPickup({ ...valid, storeName: '  ' })).toContain('門市')
    expect(validateCvsPickup({ ...valid, storeCode: 'A1' })).toContain('店號')
    expect(validateCvsPickup({ ...valid, recipientPhone: '0223456789' })).toContain('手機')
  })
})
