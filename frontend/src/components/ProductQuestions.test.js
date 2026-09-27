import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import * as questionApi from '../api/question'
import { useAuthStore } from '../stores/auth'
import ProductQuestions from './ProductQuestions.vue'

vi.mock('../api/question')

const push = vi.fn()
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRouter: () => ({ push, currentRoute: { value: { fullPath: '/products/1' } } }),
}))

const page = (content) => ({ content, totalElements: content.length })

describe('ProductQuestions', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    questionApi.listQuestions.mockResolvedValue(
      page([
        { id: 2, memberName: '王**', content: '有其他顏色嗎?', answer: '目前只有黑色', answeredAt: '2026-09-27T12:00:00', createdAt: '2026-09-27T10:00:00' },
        { id: 1, memberName: '陳**', content: '會縮水嗎?', answer: null, answeredAt: null, createdAt: '2026-09-26T10:00:00' },
      ]),
    )
  })

  function mountFor(productId = 1) {
    return mount(ProductQuestions, { props: { productId } })
  }

  it('loads and shows questions with answers or a pending hint', async () => {
    const wrapper = mountFor()
    await flushPromises()

    expect(questionApi.listQuestions).toHaveBeenCalledWith(1, { page: 0, size: 5 })
    expect(wrapper.text()).toContain('商品問答(2)')
    expect(wrapper.text()).toContain('目前只有黑色')
    expect(wrapper.text()).toContain('賣家尚未回覆')
  })

  it('sends guests to the login page instead of submitting', async () => {
    const wrapper = mountFor()
    await flushPromises()

    await wrapper.find('textarea').setValue('請問保固多久?')
    await wrapper.find('button.el-button--primary').trigger('click')

    expect(questionApi.askQuestion).not.toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/products/1' } })
  })

  it('submits the question for logged-in members and reloads the first page', async () => {
    // store 建立時從 localStorage 讀 token,所以要重建 pinia
    localStorage.setItem('member_token', 'token')
    setActivePinia(createPinia())
    expect(useAuthStore().isLoggedIn).toBe(true)
    questionApi.askQuestion.mockResolvedValue({})
    const wrapper = mountFor()
    await flushPromises()

    await wrapper.find('textarea').setValue('請問保固多久?')
    await wrapper.find('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(questionApi.askQuestion).toHaveBeenCalledWith(1, '請問保固多久?')
    expect(questionApi.listQuestions).toHaveBeenCalledTimes(2)
    expect(wrapper.find('textarea').element.value).toBe('')
  })

  it('reloads when switching to another product', async () => {
    const wrapper = mountFor(1)
    await flushPromises()

    await wrapper.setProps({ productId: 2 })
    await flushPromises()

    expect(questionApi.listQuestions).toHaveBeenLastCalledWith(2, { page: 0, size: 5 })
  })
})
