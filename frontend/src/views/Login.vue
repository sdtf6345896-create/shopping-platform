<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  email: '',
  password: '',
})

const rules = {
  email: [{ required: true, message: '請輸入 Email', trigger: 'blur' }],
  password: [{ required: true, message: '請輸入密碼', trigger: 'blur' }],
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登入成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>會員登入</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="密碼" prop="password">
          <el-input v-model="form.password" type="password" show-password @keyup.enter="handleSubmit" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">登入</el-button>
      </el-form>
      <p class="switch-link">
        還沒有帳號?<router-link to="/register">立即註冊</router-link>
      </p>
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
