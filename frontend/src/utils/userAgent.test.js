import { describe, expect, it } from 'vitest'
import { describeUserAgent, isMobileUserAgent } from './userAgent'

const CHROME_WIN = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36'
const EDGE_WIN = CHROME_WIN + ' Edg/126.0'
const SAFARI_IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1'
const FIREFOX_MAC = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14.0; rv:128.0) Gecko/20100101 Firefox/128.0'

describe('describeUserAgent', () => {
  it('names browser and system, checking Edge before Chrome', () => {
    expect(describeUserAgent(CHROME_WIN)).toBe('Chrome · Windows')
    expect(describeUserAgent(EDGE_WIN)).toBe('Edge · Windows')
    expect(describeUserAgent(SAFARI_IPHONE)).toBe('Safari · iPhone')
    expect(describeUserAgent(FIREFOX_MAC)).toBe('Firefox · macOS')
  })

  it('falls back gracefully', () => {
    expect(describeUserAgent(null)).toBe('未知裝置')
    expect(describeUserAgent('curl/8.0')).toBe('其他裝置')
  })

  it('detects mobile devices', () => {
    expect(isMobileUserAgent(SAFARI_IPHONE)).toBe(true)
    expect(isMobileUserAgent(CHROME_WIN)).toBe(false)
  })
})
