import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('member_token') || '')
  const refreshToken = ref(localStorage.getItem('member_refresh_token') || '')
  const member = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  // sessionId:這次登入的工作階段,「登入裝置」頁用來標示目前這台裝置
  function setTokens(newToken, newRefreshToken, sessionId) {
    token.value = newToken
    refreshToken.value = newRefreshToken
    if (sessionId) {
      localStorage.setItem('member_session_id', sessionId)
    } else if (!newToken) {
      localStorage.removeItem('member_session_id')
    }
    if (newToken) {
      localStorage.setItem('member_token', newToken)
    } else {
      localStorage.removeItem('member_token')
    }
    if (newRefreshToken) {
      localStorage.setItem('member_refresh_token', newRefreshToken)
    } else {
      localStorage.removeItem('member_refresh_token')
    }
  }

  async function login(credentials) {
    const data = await authApi.login(credentials)
    setTokens(data.token, data.refreshToken, data.sessionId)
    member.value = { id: data.memberId, name: data.name, email: data.email }
    return data
  }

  async function register(payload) {
    return authApi.register(payload)
  }

  async function fetchProfile() {
    member.value = await authApi.getProfile()
    return member.value
  }

  async function updateProfile(payload) {
    member.value = await authApi.updateProfile(payload)
    return member.value
  }

  async function logout() {
    const currentRefreshToken = refreshToken.value
    setTokens('', '')
    member.value = null
    if (currentRefreshToken) {
      try {
        await authApi.logout(currentRefreshToken)
      } catch {
        // 登出時 refresh token 失效與否不影響本機清除狀態
      }
    }
  }

  return { token, refreshToken, member, isLoggedIn, login, register, logout, fetchProfile, updateProfile }
})
