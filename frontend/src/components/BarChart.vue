<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] }, // [{ label, value }]
  color: { type: String, default: '#e4393c' },
  height: { type: Number, default: 200 },
  formatValue: { type: Function, default: (v) => v.toLocaleString() },
})

const maxValue = computed(() => Math.max(1, ...props.items.map((i) => i.value)))

// 資料點太多時,只挑幾個位置顯示標籤,避免擠成一團
const labelStep = computed(() => Math.max(1, Math.ceil(props.items.length / 10)))

function barHeight(value) {
  return `${(value / maxValue.value) * 100}%`
}

function showLabel(index) {
  return index % labelStep.value === 0 || index === props.items.length - 1
}
</script>

<template>
  <div class="bar-chart" :style="{ height: `${height}px` }">
    <el-empty v-if="items.length === 0" description="無資料" :image-size="50" />
    <div v-else class="bars">
      <div v-for="(item, index) in items" :key="item.label" class="bar-col">
        <div class="bar-track">
          <div
            class="bar"
            :style="{ height: barHeight(item.value), background: color }"
            :title="`${item.label}:${formatValue(item.value)}`"
          />
        </div>
        <span v-if="showLabel(index)" class="bar-label">{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bar-chart {
  width: 100%;
}

.bars {
  display: flex;
  align-items: flex-end;
  height: calc(100% - 20px);
  gap: 2px;
}

.bar-col {
  flex: 1;
  min-width: 4px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  position: relative;
}

.bar-track {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  min-height: 2px;
  border-radius: 2px 2px 0 0;
  transition: opacity 0.15s;
}

.bar:hover {
  opacity: 0.7;
}

.bar-label {
  position: absolute;
  top: 100%;
  margin-top: 4px;
  font-size: 10px;
  color: #999;
  white-space: nowrap;
  transform: rotate(-40deg) translateX(-8px);
  transform-origin: top left;
}
</style>
