import { describe, expect, it } from 'vitest'
import { filenameFromDisposition } from './download'

describe('filenameFromDisposition', () => {
  it('reads a quoted filename', () => {
    expect(filenameFromDisposition('attachment; filename="orders-20260927.csv"', 'x.csv')).toBe('orders-20260927.csv')
  })

  it('prefers the RFC 5987 encoded filename', () => {
    const header = `attachment; filename="a.csv"; filename*=UTF-8''%E8%A8%82%E5%96%AE.csv`
    expect(filenameFromDisposition(header, 'x.csv')).toBe('訂單.csv')
  })

  it('falls back when the header is missing', () => {
    expect(filenameFromDisposition(undefined, 'orders.csv')).toBe('orders.csv')
  })
})
