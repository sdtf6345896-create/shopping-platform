import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const isAdminApi = config.url?.startsWith('/admin')
  const tokenKey = isAdminApi ? 'admin_token' : 'member_token'
  const token = localStorage.getItem(tokenKey)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.success === false) {
      ElMessage.error(body.message || '請求失敗')
      return Promise.reject(new Error(body.message || '請求失敗'))
    }
    return body.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      const isAdminApi = error.config?.url?.startsWith('/admin')
      localStorage.removeItem(isAdminApi ? 'admin_token' : 'member_token')
      ElMessage.error(message || '請先登入')
      router.push(isAdminApi ? '/admin/login' : '/login')
    } else if (status === 403) {
      ElMessage.error(message || '權限不足')
    } else {
      ElMessage.error(message || '網路異常,請稍後再試')
    }
    return Promise.reject(error)
  },
)

export default request
