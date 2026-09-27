import { describe, expect, it } from 'vitest'
import { isValidMobileBarcode, isValidTaxId, validateInvoice } from './invoice'

describe('isValidTaxId (same cases as the backend InvoiceRulesTest)', () => {
  it.each(['04595257', '22099131', '10000004', '10000070'])('accepts %s', (taxId) => {
    expect(isValidTaxId(taxId)).toBe(true)
  })

  it.each(['04595258', '12345678', '10000072', '1234567', 'abcdefgh', ''])('rejects %s', (taxId) => {
    expect(isValidTaxId(taxId)).toBe(false)
  })
})

describe('isValidMobileBarcode', () => {
  it('accepts slash + 7 allowed characters', () => {
    expect(isValidMobileBarcode('/A.B+C-1')).toBe(true)
    expect(isValidMobileBarcode('/abc1234')).toBe(false)
    expect(isValidMobileBarcode('/ABC123')).toBe(false)
  })
})

describe('validateInvoice', () => {
  it('uppercases the barcode before validating', () => {
    expect(validateInvoice({ type: 'MOBILE_BARCODE', carrierCode: ' /abc1234 ' })).toBeNull()
  })

  it('requires a company title for company invoices', () => {
    expect(validateInvoice({ type: 'COMPANY', taxId: '04595257', companyTitle: ' ' })).toContain('抬頭')
    expect(validateInvoice({ type: 'COMPANY', taxId: '04595257', companyTitle: '範例公司' })).toBeNull()
  })

  it('member carrier needs nothing', () => {
    expect(validateInvoice({ type: 'MEMBER_CARRIER' })).toBeNull()
  })
})
