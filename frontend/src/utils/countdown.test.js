import { describe, expect, it } from 'vitest'
import { formatCountdown, remainingMs } from './countdown'

describe('remainingMs', () => {
  it('returns time left until the deadline', () => {
    const now = new Date('2026-09-27T15:00:00').getTime()
    expect(remainingMs('2026-09-27T15:30:00', now)).toBe(30 * 60 * 1000)
  })

  it('never goes below zero once the deadline has passed', () => {
    const now = new Date('2026-09-27T16:00:00').getTime()
    expect(remainingMs('2026-09-27T15:30:00', now)).toBe(0)
  })

  it('treats a missing deadline as expired', () => {
    expect(remainingMs(null)).toBe(0)
  })
})

describe('formatCountdown', () => {
  it('formats under an hour as mm:ss', () => {
    expect(formatCountdown(29 * 60 * 1000 + 5 * 1000)).toBe('29:05')
  })

  it('includes hours when needed', () => {
    expect(formatCountdown(3600 * 1000 + 61 * 1000)).toBe('1:01:01')
  })

  it('drops partial seconds', () => {
    expect(formatCountdown(1999)).toBe('00:01')
  })
})
