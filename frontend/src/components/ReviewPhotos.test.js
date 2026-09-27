import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewPhotos from './ReviewPhotos.vue'

describe('ReviewPhotos', () => {
  it('renders one thumbnail per photo', () => {
    const wrapper = mount(ReviewPhotos, { props: { images: ['/uploads/a.jpg', '/uploads/b.png'] } })

    expect(wrapper.findAll('.photo')).toHaveLength(2)
  })

  it('renders nothing without photos', () => {
    expect(mount(ReviewPhotos, { props: { images: [] } }).find('.review-photos').exists()).toBe(false)
  })
})
