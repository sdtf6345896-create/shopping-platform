<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyReferral } from '../../api/member'

const referral = ref(null)
const loading = ref(true)

const inviteLink = computed(() =>
  referral.value ? `${window.location.origin}/register?ref=${referral.value.code}` : '',
)

async function copy(text, label) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已複製${label}`)
  } catch {
    ElMessage.warning('無法自動複製,請手動選取複製')
  }
}

onMounted(async () => {
  try {
    referral.value = await getMyReferral()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <h3>邀請好友</h3>
    <template v-if="referral">
      <p class="intro">
        好友用你的邀請碼註冊,第一筆訂單(商品金額滿 NT$ {{ referral.minOrderAmount }})完成後,
        你可獲得 <strong>{{ referral.referrerPoints }} 點</strong>購物金,好友也能獲得
        <strong>{{ referral.refereePoints }} 點</strong>!
      </p>

      <div class="code-card">
        <div class="code-label">我的邀請碼</div>
        <div class="code">{{ referral.code }}</div>
        <el-button size="small" @click="copy(referral.code, '邀請碼')">複製邀請碼</el-button>
      </div>

      <el-form label-width="80px" class="link-form">
        <el-form-item label="邀請連結">
          <el-input :model-value="inviteLink" readonly>
            <template #append>
              <el-button @click="copy(inviteLink, '邀請連結')">複製</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="stats">
        <el-statistic title="已邀請註冊" :value="referral.invitedCount" suffix="人" />
        <el-statistic title="已完成首購" :value="referral.rewardedCount" suffix="人" />
      </div>
      <p class="hint">獎勵於好友訂單「完成」(確認收貨)時自動發放,每位好友只計一次。</p>
    </template>
  </div>
</template>

<style scoped>
.intro {
  color: #555;
  line-height: 1.7;
}

.code-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin: 16px 0;
  border: 1px dashed #f0a020;
  border-radius: 8px;
  background: #fffaf0;
  max-width: 480px;
}

.code-label {
  color: #999;
  font-size: 13px;
}

.code {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 3px;
  font-family: monospace;
}

.link-form {
  max-width: 560px;
}

.stats {
  display: flex;
  gap: 48px;
  margin: 16px 0 8px;
}

.hint {
  color: #999;
  font-size: 12px;
}
</style>
