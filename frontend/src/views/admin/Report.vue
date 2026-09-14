<script setup>
import { computed, onMounted, ref } from 'vue'
import { getSalesSummary, getTopProducts, getDailySales } from '../../api/admin/report'
import BarChart from '../../components/BarChart.vue'

const loading = ref(true)
const dateRange = ref(null) // [startDate, endDate] as 'YYYY-MM-DD' strings
const summary = ref(null)
const topProducts = ref([])
const dailySales = ref([])

const dailyChartItems = computed(() =>
  dailySales.value.map((d) => ({ label: d.date.slice(5), value: d.revenue })),
)

const statusRows = computed(() => {
  if (!summary.value) return []
  const labels = {
    PENDING_PAYMENT: '待付款',
    PAID: '已付款',
    SHIPPING: '出貨中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return Object.entries(summary.value.statusCounts).map(([key, count]) => ({
    label: labels[key] || key,
    count,
  }))
})

async function loadAll() {
  loading.value = true
  try {
    const params = dateRange.value
      ? { startDate: dateRange.value[0], endDate: dateRange.value[1] }
      : {}

    const [summaryData, topProductsData, dailyData] = await Promise.all([
      getSalesSummary(params),
      getTopProducts({ ...params, limit: 10 }),
      getDailySales(params),
    ])

    summary.value = summaryData
    topProducts.value = topProductsData
    dailySales.value = dailyData
    dateRange.value = [summaryData.startDate, summaryData.endDate]
  } finally {
    loading.value = false
  }
}

function handleRangeChange() {
  loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div v-loading="loading">
    <div class="header-row">
      <h3>銷售報表</h3>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="開始日期"
        end-placeholder="結束日期"
        :clearable="false"
        @change="handleRangeChange"
      />
    </div>

    <template v-if="summary">
      <div class="stat-grid">
        <el-card class="stat-card">
          <el-statistic title="總訂單數" :value="summary.totalOrders" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="已付款訂單數" :value="summary.paidOrders" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="已取消訂單數" :value="summary.cancelledOrders" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="總營收" :value="summary.totalRevenue" prefix="NT$" />
        </el-card>
        <el-card class="stat-card">
          <el-statistic title="平均客單價" :value="summary.averageOrderValue" prefix="NT$" />
        </el-card>
      </div>

      <div class="status-tags">
        <el-tag v-for="row in statusRows" :key="row.label" class="status-tag">
          {{ row.label }}:{{ row.count }}
        </el-tag>
      </div>

      <div class="block">
        <div class="block-title">每日營收趨勢</div>
        <BarChart :items="dailyChartItems" :height="220" />
      </div>

      <div class="block">
        <div class="block-title">熱銷商品排行(依銷售數量)</div>
        <el-table :data="topProducts">
          <el-table-column label="排名" width="70">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="縮圖" width="70">
            <template #default="{ row }">
              <img v-if="row.mainImage" :src="row.mainImage" class="thumb" />
              <div v-else class="thumb placeholder">無</div>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名稱" min-width="200" />
          <el-table-column prop="soldQuantity" label="銷售數量" width="100" />
          <el-table-column label="銷售金額" width="120">
            <template #default="{ row }">NT$ {{ row.revenue }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="topProducts.length === 0" description="此區間無銷售資料" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.status-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 20px;
}

.thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  background: #f5f5f5;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 11px;
}
</style>
