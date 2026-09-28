import { describe, expect, it } from 'vitest'
import { parseEmailList } from './emailList'

describe('parseEmailList', () => {
  it('splits on newlines, commas, semicolons and spaces, lowercasing and de-duplicating', () => {
    const text = 'Alice@Example.com\r\nbob@example.com, carol@example.com;dave@example.com  alice@example.com\n\n'
    expect(parseEmailList(text)).toEqual([
      'alice@example.com',
      'bob@example.com',
      'carol@example.com',
      'dave@example.com',
    ])
  })

  it('returns an empty list for blank input', () => {
    expect(parseEmailList('  \n ')).toEqual([])
    expect(parseEmailList(null)).toEqual([])
  })
})
