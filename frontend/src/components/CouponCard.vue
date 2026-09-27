<script setup>
import { formatDiscount } from '../utils/couponEnums'

defineProps({
  coupon: { type: Object, required: true },
})
</script>

<template>
  <div class="coupon-card">
    <div class="left">
      <p class="discount">{{ formatDiscount(coupon) }}</p>
      <p class="min-spend">{{ Number(coupon.minSpendAmount) > 0 ? `滿 NT$ ${coupon.minSpendAmount} 可用` : '無門檻' }}</p>
    </div>
    <div class="right">
      <p class="name">{{ coupon.name }}</p>
      <p class="code">代碼:{{ coupon.code }}</p>
      <p class="expiry">{{ coupon.endAt ? `使用期限至 ${coupon.endAt.slice(0, 10)}` : '無使用期限' }}</p>
      <div class="actions">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.coupon-card {
  display: flex;
  border: 1px solid #f5c2c3;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.left {
  width: 120px;
  flex-shrink: 0;
  padding: 16px 12px;
  background: #e4393c;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.discount {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.min-spend {
  margin: 6px 0 0;
  font-size: 12px;
  opacity: 0.9;
}

.right {
  flex: 1;
  padding: 12px 16px;
  min-width: 0;
}

.name {
  margin: 0;
  font-weight: 600;
}

.code,
.expiry {
  margin: 4px 0 0;
  font-size: 12px;
  color: #999;
}

.actions {
  margin-top: 8px;
}
</style>
