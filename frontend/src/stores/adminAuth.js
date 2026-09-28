import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import * as adminAuthApi from '../api/admin/auth'
import { getMyAdminAccount } from '../api/admin/account'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const admin = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => admin.value?.role || null)
  const isAdmin = computed(() => role.value === 'ADMIN')

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
    admin.value = { id: data.adminId, username: data.username, name: data.name, role: data.role }
    return data
  }

  // 重新整理後只剩 token,向後端取回帳號與角色(角色以後端為準,調整後重新整理即生效)
  async function fetchMe() {
    const me = await getMyAdminAccount()
    admin.value = { id: me.id, username: me.username, name: me.name, role: me.role }
    return admin.value
  }

  function logout() {
    setToken('')
    admin.value = null
  }

  return { token, admin, role, isAdmin, isLoggedIn, login, logout, fetchMe }
})
