<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyTier } from '../api/member'

const TIER_COLORS = { NORMAL: '#909399', SILVER: '#8a9bb0', GOLD: '#d4a017' }

const tier = ref(null)

// 升級進度:本級門檻到下一級門檻之間的比例
const progress = computed(() => {
  if (!tier.value?.nextTier) return 100
  const spending = Number(tier.value.spending)
  const toNext = Number(tier.value.amountToNext)
  return Math.min(100, Math.round((spending / (spending + toNext)) * 100))
})

onMounted(async () => {
  try {
    tier.value = await getMyTier()
  } catch {
    tier.value = null
  }
})
</script>

<template>
  <div v-if="tier" class="tier-card" :style="{ borderColor: TIER_COLORS[tier.tier] }">
    <div class="tier-head">
      <span class="tier-badge" :style="{ background: TIER_COLORS[tier.tier] }">{{ tier.label }}</span>
      <span class="multiplier">購物金回饋 {{ Number(tier.pointsMultiplier) }} 倍</span>
    </div>
    <p class="spending">近 12 個月消費 NT$ {{ Number(tier.spending).toLocaleString() }}</p>
    <template v-if="tier.nextTier">
      <el-progress :percentage="progress" :show-text="false" :stroke-width="6" :color="TIER_COLORS[tier.nextTier]" />
      <p class="hint">再消費 NT$ {{ Number(tier.amountToNext).toLocaleString() }} 升級為{{ tier.nextLabel }}</p>
    </template>
    <p v-else class="hint">已是最高等級,感謝您的支持!</p>
  </div>
</template>

<style scoped>
.tier-card {
  border: 1px solid;
  border-left-width: 4px;
  border-radius: 8px;
  padding: 14px 16px;
  margin: 12px 0 20px;
  background: #fff;
}

.tier-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tier-badge {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.multiplier {
  color: #666;
  font-size: 13px;
}

.spending {
  margin: 8px 0;
  font-size: 14px;
}

.hint {
  margin: 6px 0 0;
  color: #999;
  font-size: 12px;
}
</style>
