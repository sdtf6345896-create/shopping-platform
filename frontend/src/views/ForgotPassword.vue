<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as authApi from '../api/auth'

const router = useRouter()

const formRef = ref()
const loading = ref(false)
const submitted = ref(false)
const form = reactive({
  email: '',
})

const rules = {
  email: [
    { required: true, message: '請輸入 Email', trigger: 'blur' },
    { type: 'email', message: 'Email 格式不正確', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authApi.forgotPassword(form)
    submitted.value = true
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>忘記密碼</h2>
      <template v-if="submitted">
        <el-alert type="success" :closable="false" show-icon title="重設密碼信已送出">
          若該 Email 已註冊,重設密碼連結將寄送過去,請留意收件匣。
        </el-alert>
        <p class="switch-link">
          <router-link to="/login">返回登入</router-link>
        </p>
      </template>
      <template v-else>
        <p class="hint">請輸入註冊時使用的 Email,我們會寄送重設密碼連結給你。</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="Email" prop="email">
            <el-input v-model="form.email" placeholder="you@example.com" @keyup.enter="handleSubmit" />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
            寄送重設密碼信
          </el-button>
        </el-form>
        <p class="switch-link">
          想起密碼了?<router-link to="/login">返回登入</router-link>
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
