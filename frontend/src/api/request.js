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

// 換發 access token 用獨立的 axios 呼叫,避免又經過這支攔截器造成無窮迴圈
let refreshPromise = null

function refreshMemberToken() {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem('member_refresh_token')
    if (!refreshToken) {
      return Promise.reject(new Error('無 refresh token'))
    }
    refreshPromise = axios
      .post('/api/auth/refresh', { refreshToken })
      .then((res) => {
        const data = res.data.data
        localStorage.setItem('member_token', data.token)
        localStorage.setItem('member_refresh_token', data.refreshToken)
        return data.token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

request.interceptors.response.use(
  (response) => {
    // 檔案下載回傳完整 response,呼叫端需要讀 Content-Disposition 取檔名
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && body.success === false) {
      ElMessage.error(body.message || '請求失敗')
      return Promise.reject(new Error(body.message || '請求失敗'))
    }
    return body.data
  },
  async (error) => {
    const status = error.response?.status
    let message = error.response?.data?.message
    // 下載檔案失敗時,錯誤內容也是 Blob,要自己解析 JSON
    if (error.response?.data instanceof Blob) {
      try {
        message = JSON.parse(await error.response.data.text()).message
      } catch {
        // 不是 JSON 就用預設訊息
      }
    }
    const originalRequest = error.config
    const isAdminApi = originalRequest?.url?.startsWith('/admin')
    const isRefreshCall = originalRequest?.url?.startsWith('/auth/refresh')

    if (status === 401 && !isAdminApi && !isRefreshCall && !originalRequest._retry) {
      originalRequest._retry = true
      try {
        const newToken = await refreshMemberToken()
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return request(originalRequest)
      } catch {
        localStorage.removeItem('member_token')
        localStorage.removeItem('member_refresh_token')
        ElMessage.error('請先登入')
        router.push('/login')
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      const tokenKey = isAdminApi ? 'admin_token' : 'member_token'
      localStorage.removeItem(tokenKey)
      if (!isAdminApi) {
        localStorage.removeItem('member_refresh_token')
      }
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
