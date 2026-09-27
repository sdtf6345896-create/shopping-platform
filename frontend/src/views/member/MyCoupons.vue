<script setup>
import { onMounted, ref } from 'vue'
import { getMyCoupons } from '../../api/coupon'
import CouponCard from '../../components/CouponCard.vue'

const coupons = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    coupons.value = await getMyCoupons()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="header-row">
      <h3>我的優惠券</h3>
      <router-link to="/coupons">去領券中心 →</router-link>
    </div>
    <p class="hint">只列出目前可使用的優惠券(已過期、已停用或已用完的不會顯示)。</p>
    <el-empty v-if="!loading && coupons.length === 0" description="還沒有可用的優惠券" :image-size="60" />
    <div class="coupon-list">
      <CouponCard v-for="coupon in coupons" :key="coupon.id" :coupon="coupon" />
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  color: #999;
  font-size: 13px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}
</style>
