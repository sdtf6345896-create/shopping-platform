<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard } from '../../api/admin/report'
import { getHotSearches } from '../../api/product'

const router = useRouter()
const loading = ref(true)
const data = ref(null)
const hotSearches = ref([])

// 待處理事項:數字 > 0 才醒目標示,點擊直接帶到對應的篩選頁面
const todos = computed(() => {
  if (!data.value) return []
  const d = data.value
  return [
    { label: '待出貨訂單', value: d.ordersToShip, to: { name: 'AdminOrderList', query: { status: 'PAID' } } },
    { label: '待審核退貨', value: d.pendingReturns, to: { name: 'AdminReturnList' } },
    { label: '待回覆提問', value: d.unansweredQuestions, to: { name: 'AdminQuestionList' } },
    { label: `低庫存規格(≤${d.lowStockThreshold})`, value: d.lowStockSkus, to: { name: 'AdminReport' } },
    {
      label: '待付款訂單',
      value: d.pendingPaymentOrders,
      to: { name: 'AdminOrderList', query: { status: 'PENDING_PAYMENT' } },
      muted: true,
    },
  ]
})

onMounted(async () => {
  try {
    const [dashboard, hot] = await Promise.all([getDashboard(), getHotSearches(10).catch(() => [])])
    data.value = dashboard
    hotSearches.value = hot
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <h3>總覽</h3>

    <template v-if="data">
      <div class="today-grid">
        <el-card class="stat-card">
          <el-statistic title="今日訂單" :value="data.todayOrders" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="今日營收" :value="data.todayRevenue" prefix="NT$" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="今日新會員" :value="data.todayNewMembers" />
        </el-card>
      </div>

      <h4 class="section-title">待處理事項</h4>
      <div class="todo-grid">
        <div
          v-for="todo in todos"
          :key="todo.label"
          class="todo-card"
          :class="{ active: todo.value > 0 && !todo.muted }"
          role="link"
          tabindex="0"
          @click="router.push(todo.to)"
          @keyup.enter="router.push(todo.to)"
        >
          <span class="todo-value">{{ todo.value }}</span>
          <span class="todo-label">{{ todo.label }}</span>
        </div>
      </div>

      <h4 class="section-title hot-title">熱門搜尋(近 30 天)</h4>
      <div class="hot-searches">
        <el-tag v-for="(keyword, index) in hotSearches" :key="keyword" effect="plain" round>
          {{ index + 1 }}. {{ keyword }}
        </el-tag>
        <span v-if="hotSearches.length === 0" class="empty">尚無搜尋紀錄</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.today-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin: 12px 0 24px;
}

.stat-card {
  text-align: center;
}

.section-title {
  margin: 0 0 12px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.todo-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.todo-card:hover,
.todo-card:focus-visible {
  border-color: #e4393c;
  box-shadow: 0 2px 8px rgba(228, 57, 60, 0.12);
  outline: none;
}

.todo-value {
  font-size: 28px;
  font-weight: 700;
  color: #999;
}

.todo-card.active .todo-value {
  color: #e4393c;
}

.hot-title {
  margin-top: 24px;
}

.hot-searches {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty {
  color: #999;
  font-size: 13px;
}

.todo-label {
  font-size: 13px;
  color: #666;
}
</style>
