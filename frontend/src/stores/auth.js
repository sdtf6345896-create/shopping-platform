import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('member_token') || '')
  const member = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('member_token', newToken)
    } else {
      localStorage.removeItem('member_token')
    }
  }

  async function login(credentials) {
    const data = await authApi.login(credentials)
    setToken(data.token)
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

  function logout() {
    setToken('')
    member.value = null
  }

  return { token, member, isLoggedIn, login, register, logout, fetchProfile, updateProfile }
})
