<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { claimCoupon, getCouponCenter } from '../api/coupon'
import CouponCard from '../components/CouponCard.vue'

const coupons = ref([])
const loading = ref(true)
const claimingId = ref(null)

async function handleClaim(coupon) {
  claimingId.value = coupon.id
  try {
    await claimCoupon(coupon.id)
    coupon.claimed = true
    ElMessage.success('領取成功,結帳時可直接選用')
  } finally {
    claimingId.value = null
  }
}

onMounted(async () => {
  try {
    coupons.value = await getCouponCenter()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="page-container">
    <h2>領券中心</h2>
    <p class="hint">領取後可在「會員中心 → 我的優惠券」查看,結帳時直接選用。</p>
    <el-empty v-if="!loading && coupons.length === 0" description="目前沒有可領取的優惠券" />
    <div class="coupon-grid">
      <CouponCard v-for="coupon in coupons" :key="coupon.id" :coupon="coupon">
        <el-button v-if="coupon.claimed" size="small" disabled>已領取</el-button>
        <el-button
          v-else
          size="small"
          type="danger"
          :loading="claimingId === coupon.id"
          @click="handleClaim(coupon)"
        >
          立即領取
        </el-button>
      </CouponCard>
    </div>
  </div>
</template>

<style scoped>
.hint {
  color: #999;
  font-size: 13px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
</style>
