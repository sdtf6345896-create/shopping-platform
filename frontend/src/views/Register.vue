<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  email: '',
  password: '',
  name: '',
  phone: '',
})

const rules = {
  email: [
    { required: true, message: '請輸入 Email', trigger: 'blur' },
    { type: 'email', message: 'Email 格式不正確', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '請輸入密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少 8 碼', trigger: 'blur' },
  ],
  name: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.register(form)
    ElMessage.success('註冊成功,請登入')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>會員註冊</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="密碼" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手機號碼(選填)" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">註冊</el-button>
      </el-form>
      <p class="switch-link">
        已經有帳號?<router-link to="/login">前往登入</router-link>
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
