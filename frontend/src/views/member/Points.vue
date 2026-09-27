<script setup>
import { onMounted, ref } from 'vue'
import { getPointBalance, listPointTransactions } from '../../api/points'
import { POINT_TYPE_LABELS } from '../../utils/points'

const balance = ref(null)
const transactions = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(true)

async function loadTransactions() {
  loading.value = true
  try {
    const data = await listPointTransactions({ page: page.value, size: 10 })
    transactions.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  loadTransactions()
}

onMounted(async () => {
  balance.value = await getPointBalance()
  await loadTransactions()
})
</script>

<template>
  <div>
    <h3>我的購物金</h3>

    <div v-if="balance" class="balance-card">
      <div>
        <p class="label">可用購物金</p>
        <p class="amount">{{ balance.balance }} <span class="unit">點</span></p>
      </div>
      <ul class="rules">
        <li>1 點可折抵 NT$1</li>
        <li>訂單完成後回饋實付金額的 {{ Math.round(balance.earnRate * 100) }}%</li>
        <li>每筆訂單最多折抵應付金額的 {{ Math.round(balance.maxRedeemRatio * 100) }}%</li>
        <li>訂單取消時,已使用的購物金會退回</li>
      </ul>
    </div>

    <el-table v-loading="loading" :data="transactions" class="table">
      <el-table-column label="時間" width="170">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="類型" width="100">
        <template #default="{ row }">{{ POINT_TYPE_LABELS[row.type] || row.type }}</template>
      </el-table-column>
      <el-table-column label="說明" min-width="200">
        <template #default="{ row }">
          <router-link v-if="row.orderId" :to="`/orders/${row.orderId}`">{{ row.description }}</router-link>
          <span v-else>{{ row.description }}</span>
        </template>
      </el-table-column>
      <el-table-column label="點數" width="90" align="right">
        <template #default="{ row }">
          <span :class="row.amount > 0 ? 'plus' : 'minus'">{{ row.amount > 0 ? '+' : '' }}{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="balanceAfter" label="餘額" width="90" align="right" />
    </el-table>
    <el-empty v-if="!loading && transactions.length === 0" description="還沒有購物金紀錄" :image-size="60" />

    <el-pagination
      v-if="total > 10"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.balance-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background: linear-gradient(135deg, #fff5f5, #fff);
  border: 1px solid #f5d0d0;
  border-radius: 8px;
  padding: 20px 24px;
  margin: 12px 0 20px;
}

.label {
  margin: 0;
  color: #666;
  font-size: 13px;
}

.amount {
  margin: 4px 0 0;
  font-size: 32px;
  font-weight: 700;
  color: #e4393c;
}

.unit {
  font-size: 14px;
  font-weight: normal;
}

.rules {
  margin: 0;
  padding-left: 18px;
  color: #666;
  font-size: 13px;
  line-height: 1.8;
}

.plus {
  color: #67c23a;
}

.minus {
  color: #e4393c;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
