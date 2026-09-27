<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as authApi from '../api/auth'

const route = useRoute()

const status = ref(route.query.token ? 'verifying' : 'idle')

const resendFormRef = ref()
const resendLoading = ref(false)
const resendSubmitted = ref(false)
const resendForm = reactive({ email: route.query.email || '' })

const resendRules = {
  email: [
    { required: true, message: '請輸入 Email', trigger: 'blur' },
    { type: 'email', message: 'Email 格式不正確', trigger: 'blur' },
  ],
}

async function verify() {
  try {
    await authApi.verifyEmail(route.query.token)
    status.value = 'success'
  } catch {
    status.value = 'error'
  }
}

async function handleResend() {
  await resendFormRef.value.validate()
  resendLoading.value = true
  try {
    await authApi.resendVerification(resendForm.email)
    resendSubmitted.value = true
  } finally {
    resendLoading.value = false
  }
}

onMounted(() => {
  if (status.value === 'verifying') {
    verify()
  }
})
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>Email 驗證</h2>

      <template v-if="status === 'verifying'">
        <el-alert type="info" :closable="false" show-icon title="驗證中,請稍候..." />
      </template>

      <template v-else-if="status === 'success'">
        <el-alert type="success" :closable="false" show-icon title="Email 驗證成功">
          請重新登入。
        </el-alert>
        <p class="switch-link">
          <router-link to="/login">前往登入</router-link>
        </p>
      </template>

      <template v-else>
        <el-alert v-if="status === 'error'" type="error" :closable="false" show-icon
          title="驗證連結無效或已過期" class="mb" />

        <template v-if="resendSubmitted">
          <el-alert type="success" :closable="false" show-icon title="驗證信已重新送出">
            若該 Email 尚未驗證,請至信箱查收新的驗證連結。
          </el-alert>
        </template>
        <template v-else>
          <p v-if="route.query.email" class="hint">
            我們已寄送驗證信到 {{ route.query.email }},請至信箱完成驗證。若沒收到,可重新寄送。
          </p>
          <p v-else class="hint">請輸入註冊時使用的 Email,重新寄送驗證信。</p>
          <el-form ref="resendFormRef" :model="resendForm" :rules="resendRules" label-position="top"
            @submit.prevent="handleResend">
            <el-form-item label="Email" prop="email">
              <el-input v-model="resendForm.email" placeholder="you@example.com" @keyup.enter="handleResend" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="resendLoading" @click="handleResend">
              重新寄送驗證信
            </el-button>
          </el-form>
        </template>
        <p class="switch-link">
          <router-link to="/login">返回登入</router-link>
        </p>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 60px 16px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
}

.auth-card h2 {
  text-align: center;
  margin-bottom: 24px;
}

.hint {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.mb {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
}

.switch-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.switch-link a {
  color: #e4393c;
}
</style>
