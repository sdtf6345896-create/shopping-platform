<script setup>
// 後台發券:把優惠券直接放進會員的「我的優惠券」(依全部 / 等級 / Email 名單)
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { issueCoupon } from '../api/admin/coupon'
import { parseEmailList } from '../utils/emailList'

const props = defineProps({
  coupon: { type: Object, default: null },
})
const emit = defineEmits(['close'])

const TIERS = [
  { value: 'SILVER', label: '銀卡會員以上' },
  { value: 'GOLD', label: '金卡會員' },
]

const form = reactive({ target: 'TIER', minTier: 'GOLD', emailsText: '' })
const submitting = ref(false)
const result = ref(null)
const emails = computed(() => parseEmailList(form.emailsText))

watch(
  () => props.coupon,
  () => {
    Object.assign(form, { target: 'TIER', minTier: 'GOLD', emailsText: '' })
    result.value = null
  },
)

async function handleSubmit() {
  if (form.target === 'EMAILS' && emails.value.length === 0) {
    ElMessage.warning('請輸入至少一個會員 Email')
    return
  }
  submitting.value = true
  try {
    result.value = await issueCoupon(props.coupon.id, {
      target: form.target,
      minTier: form.target === 'TIER' ? form.minTier : null,
      emails: form.target === 'EMAILS' ? emails.value : null,
    })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="!!coupon" title="發放優惠券" width="520px" @close="emit('close')">
    <template v-if="coupon">
      <p class="coupon-line">
        <strong>{{ coupon.name }}</strong>
        <el-tag size="small">{{ coupon.code }}</el-tag>
      </p>

      <template v-if="!result">
        <el-form label-width="80px">
          <el-form-item label="發放對象">
            <el-radio-group v-model="form.target">
              <el-radio value="TIER">依會員等級</el-radio>
              <el-radio value="EMAILS">指定會員</el-radio>
              <el-radio value="ALL">全部會員</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.target === 'TIER'" label="等級">
            <el-select v-model="form.minTier" style="width: 200px">
              <el-option v-for="t in TIERS" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.target === 'EMAILS'" label="Email">
            <el-input
              v-model="form.emailsText"
              type="textarea"
              :rows="6"
              placeholder="每行一個,或用逗號分隔(最多 1000 個)"
            />
            <div class="hint">已辨識 {{ emails.length }} 個 Email</div>
          </el-form-item>
        </el-form>
        <p class="hint">已持有此券的會員會自動略過,不會重複發放或通知。會員會收到站內通知。</p>
      </template>

      <template v-else>
        <el-alert
          type="success"
          :closable="false"
          :title="`已發放給 ${result.issued} 位會員`"
          :description="`符合條件 ${result.targeted} 人,其中 ${result.alreadyHeld} 人原本就持有`"
        />
        <div v-if="result.unmatchedEmails.length" class="unmatched">
          <p>以下 {{ result.unmatchedEmails.length }} 個 Email 找不到會員或帳號已停用:</p>
          <el-input :model-value="result.unmatchedEmails.join('\n')" type="textarea" :rows="4" readonly />
        </div>
      </template>
    </template>

    <template #footer>
      <el-button @click="emit('close')">{{ result ? '關閉' : '取消' }}</el-button>
      <el-button v-if="!result" type="primary" :loading="submitting" @click="handleSubmit">發放</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.coupon-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
}

.hint {
  color: #999;
  font-size: 12px;
}

.unmatched {
  margin-top: 12px;
  font-size: 13px;
}
</style>
