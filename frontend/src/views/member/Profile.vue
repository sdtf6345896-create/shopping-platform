<script setup>
import MemberTierCard from '../../components/MemberTierCard.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { changePassword, deleteAccount } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const formRef = ref()
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  email: '',
  name: '',
  phone: '',
  birthday: null,
})
// 生日設定後不可自行修改(後端也會擋),避免反覆改生日領生日禮
const birthdayLocked = ref(false)

function disabledBirthday(date) {
  return date.getTime() >= Date.now() || date.getFullYear() < 1900
}

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
    form.birthday = profile.birthday || null
    birthdayLocked.value = !!profile.birthday
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await authStore.updateProfile({ name: form.name, phone: form.phone, birthday: form.birthday })
    birthdayLocked.value = !!form.birthday
    ElMessage.success('更新成功')
  } finally {
    saving.value = false
  }
}

const router = useRouter()
const passwordFormRef = ref()
const changingPassword = ref(false)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  currentPassword: [{ required: true, message: '請輸入目前密碼', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '請輸入新密碼', trigger: 'blur' },
    { min: 8, message: '密碼長度至少 8 碼', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) =>
        value === passwordForm.newPassword ? callback() : callback(new Error('兩次輸入的新密碼不一致')),
      trigger: 'blur',
    },
  ],
}

async function handleChangePassword() {
  await passwordFormRef.value.validate()
  changingPassword.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    // 後端已撤銷所有登入狀態,直接登出並請使用者用新密碼重新登入
    authStore.logout()
    ElMessage.success('密碼已變更,請使用新密碼重新登入')
    router.push({ name: 'Login' })
  } finally {
    changingPassword.value = false
  }
}

async function handleDeleteAccount() {
  let password
  try {
    const { value } = await ElMessageBox.prompt(
      '刪除後個人資料將被清除、無法復原,訂單紀錄會以匿名方式保留。請輸入密碼確認:',
      '刪除帳號',
      {
        type: 'error',
        inputType: 'password',
        confirmButtonText: '永久刪除',
        confirmButtonClass: 'el-button--danger',
        inputValidator: (v) => !!v || '請輸入密碼',
      },
    )
    password = value
  } catch {
    return
  }
  await deleteAccount(password)
  authStore.logout()
  ElMessage.success('帳號已刪除,感謝您曾經的支持')
  router.push('/')
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <h3>個人資料</h3>
    <MemberTierCard />
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
      <el-form-item label="生日">
        <el-date-picker
          v-model="form.birthday"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="選擇生日"
          :disabled="birthdayLocked"
          :disabled-date="disabledBirthday"
          :default-value="new Date(1995, 0, 1)"
        />
        <div class="hint-block birthday-hint">
          {{ birthdayLocked ? '生日設定後無法修改,如需更正請聯絡客服' : '生日當月送購物金!設定後無法修改,請確認正確' }}
        </div>
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="handleSave">儲存變更</el-button>
    </el-form>

    <h3 class="section-title">修改密碼</h3>
    <el-form
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-width="100px"
      class="profile-form"
      @submit.prevent
    >
      <el-form-item label="目前密碼" prop="currentPassword">
        <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密碼" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="確認新密碼" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">變更密碼</el-button>
        <span class="hint">變更後所有裝置都需要重新登入</span>
      </el-form-item>
    </el-form>

    <div class="danger-zone">
      <h3 class="section-title danger-title">刪除帳號</h3>
      <p class="hint-block">
        刪除後 Email、姓名、電話與收件地址會被清除,購物車、收藏、通知一併移除,且無法復原。
        仍有處理中的訂單或退貨時無法刪除。
      </p>
      <el-button type="danger" plain @click="handleDeleteAccount">刪除我的帳號</el-button>
    </div>
  </div>
</template>

<style scoped>
.danger-zone {
  margin-top: 40px;
  padding-top: 8px;
  border-top: 1px dashed #f3c6c6;
}

.danger-title {
  color: #e4393c;
}

.hint-block {
  color: #999;
  font-size: 13px;
  margin: 0 0 12px;
}

.birthday-hint {
  margin: 4px 0 0;
  line-height: 1.4;
}

.section-title {
  margin-top: 32px;
}

.hint {
  margin-left: 12px;
  color: #999;
  font-size: 12px;
}

.profile-form {
  max-width: 400px;
}
</style>
