<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const formRef = ref()
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  email: '',
  name: '',
  phone: '',
})

const rules = {
  name: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const profile = await authStore.fetchProfile()
    form.email = profile.email
    form.name = profile.name
    form.phone = profile.phone
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await authStore.updateProfile({ name: form.name, phone: form.phone })
    ElMessage.success('更新成功')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <h3>個人資料</h3>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="profile-form">
      <el-form-item label="Email">
        <el-input v-model="form.email" disabled />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="手機">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="handleSave">儲存變更</el-button>
    </el-form>
  </div>
</template>

<style scoped>
.profile-form {
  max-width: 400px;
}
</style>
