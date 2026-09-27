<script setup>
import { computed } from 'vue'
import { ORDER_ACTOR_LABELS, ORDER_STATUS_LABELS } from '../utils/orderEnums'

const props = defineProps({
  logs: { type: Array, default: () => [] },
  // 前台不需要顯示是誰操作的
  showActor: { type: Boolean, default: false },
})

const TIMELINE_TYPES = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  SHIPPING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  REFUNDED: 'danger',
}

// 最新的狀態排最上面
const entries = computed(() => [...props.logs].reverse())
</script>

<template>
  <el-timeline v-if="entries.length" class="order-timeline">
    <el-timeline-item
      v-for="(log, index) in entries"
      :key="index"
      :type="TIMELINE_TYPES[log.toStatus]"
      :hollow="index !== 0"
      :timestamp="log.createdAt?.slice(0, 19).replace('T', ' ')"
    >
      <span class="status">{{ ORDER_STATUS_LABELS[log.toStatus] }}</span>
      <span v-if="showActor" class="actor">({{ ORDER_ACTOR_LABELS[log.actor] }})</span>
      <div v-if="log.note" class="note">{{ log.note }}</div>
    </el-timeline-item>
  </el-timeline>
  <p v-else class="empty">尚無狀態紀錄</p>
</template>

<style scoped>
.order-timeline {
  padding-left: 4px;
  margin: 0;
}

.status {
  font-weight: 600;
}

.actor {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}

.note {
  color: #666;
  font-size: 13px;
  margin-top: 4px;
}

.empty {
  color: #999;
  font-size: 13px;
}
</style>
