import { beforeEach, describe, expect, it } from 'vitest'
import { DEFAULT_DESCRIPTION, SITE_NAME, setMetaDescription, setPageTitle } from './pageMeta'

describe('pageMeta', () => {
  beforeEach(() => {
    document.head.innerHTML = ''
  })

  it('sets titles with the site name suffix', () => {
    setPageTitle('經典圓領T恤')
    expect(document.title).toBe(`經典圓領T恤 | ${SITE_NAME}`)
    setPageTitle(null)
    expect(document.title).toBe(SITE_NAME)
  })

  it('creates or updates the description tag, collapsing whitespace and truncating', () => {
    setMetaDescription('  純棉\n\n舒適   透氣  ')
    expect(document.querySelector('meta[name="description"]').getAttribute('content')).toBe('純棉 舒適 透氣')

    setMetaDescription('字'.repeat(200))
    expect(document.querySelectorAll('meta[name="description"]')).toHaveLength(1)
    expect(document.querySelector('meta[name="description"]').getAttribute('content')).toHaveLength(150)

    setMetaDescription('')
    expect(document.querySelector('meta[name="description"]').getAttribute('content')).toBe(DEFAULT_DESCRIPTION)
  })
})
