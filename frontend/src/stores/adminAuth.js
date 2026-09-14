import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import * as adminAuthApi from '../api/admin/auth'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const admin = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('admin_token', newToken)
    } else {
      localStorage.removeItem('admin_token')
    }
  }

  async function login(credentials) {
    const data = await adminAuthApi.adminLogin(credentials)
    setToken(data.token)
    admin.value = { id: data.adminId, username: data.username, name: data.name }
    return data
  }

  function logout() {
    setToken('')
    admin.value = null
  }

  return { token, admin, isLoggedIn, login, logout }
})
