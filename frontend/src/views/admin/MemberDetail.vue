<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminMember, getAdminMemberTier, listAdminMemberPoints } from '../../api/admin/member'
import { listAdminOrders } from '../../api/admin/order'
import { ORDER_STATUS_LABELS, ORDER_STATUS_TAG_TYPES } from '../../utils/orderEnums'
import { POINT_TYPE_LABELS } from '../../utils/points'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()
const member = ref(null)
const tier = ref(null)
const orders = ref([])
const ordersTotal = ref(0)
const ordersPage = ref(0)
const points = ref([])
const pointsTotal = ref(0)
const pointsPage = ref(0)
const loading = ref(true)

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

async function loadOrders() {
  const data = await listAdminOrders({ memberId: props.id, page: ordersPage.value, size: 5, sort: 'createdAt,desc' })
  orders.value = data.content
  ordersTotal.value = data.totalElements
}

async function loadPoints() {
  const data = await listAdminMemberPoints(props.id, { page: pointsPage.value, size: 5 })
  points.value = data.content
  pointsTotal.value = data.totalElements
}

onMounted(async () => {
  try {
    const [memberData, tierData] = await Promise.all([getAdminMember(props.id), getAdminMemberTier(props.id)])
    member.value = memberData
    tier.value = tierData
    await Promise.all([loadOrders(), loadPoints()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <router-link :to="{ name: 'AdminMemberList' }" class="back-link">← 返回會員管理</router-link>

    <template v-if="member">
      <div class="summary">
        <div class="block">
          <div class="block-title">
            <span>{{ member.name }}</span>
            <el-tag :type="member.status === 'ACTIVE' ? 'success' : 'info'">
              {{ member.status === 'ACTIVE' ? '啟用中' : '已停用' }}
            </el-tag>
          </div>
          <p class="meta">Email:{{ member.email }}</p>
          <p class="meta">手機:{{ member.phone || '-' }}</p>
          <p class="meta">註冊時間:{{ formatTime(member.createdAt) }}</p>
        </div>
        <div v-if="tier" class="block">
          <div class="block-title">會員等級與購物金</div>
          <p class="meta">等級:<strong>{{ tier.label }}</strong>(回饋 {{ Number(tier.pointsMultiplier) }} 倍)</p>
          <p class="meta">近 12 個月消費:NT$ {{ Number(tier.spending).toLocaleString() }}</p>
          <p class="meta">購物金餘額:<strong>{{ member.points }}</strong> 點</p>
        </div>
      </div>

      <div class="block">
        <div class="block-title">訂單({{ ordersTotal }})</div>
        <el-table :data="orders" size="small">
          <el-table-column label="訂單編號" min-width="170">
            <template #default="{ row }">
              <el-link type="primary" :underline="false"
                       @click="router.push({ name: 'AdminOrderDetail', params: { id: row.id } })">
                {{ row.orderNo }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="金額" width="110">
            <template #default="{ row }">NT$ {{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="狀態" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="ORDER_STATUS_TAG_TYPES[row.status]">{{ ORDER_STATUS_LABELS[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下單時間" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="ordersTotal > 5"
          class="pagination"
          small
          layout="prev, pager, next"
          :total="ordersTotal"
          :page-size="5"
          :current-page="ordersPage + 1"
          @current-change="(p) => { ordersPage = p - 1; loadOrders() }"
        />
      </div>

      <div class="block">
        <div class="block-title">購物金異動</div>
        <el-table :data="points" size="small">
          <el-table-column label="時間" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="類型" width="100">
            <template #default="{ row }">{{ POINT_TYPE_LABELS[row.type] || row.type }}</template>
          </el-table-column>
          <el-table-column prop="description" label="說明" min-width="200" />
          <el-table-column label="點數" width="80" align="right">
            <template #default="{ row }">{{ row.amount > 0 ? '+' : '' }}{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="餘額" width="80" align="right" />
        </el-table>
        <el-pagination
          v-if="pointsTotal > 5"
          class="pagination"
          small
          layout="prev, pager, next"
          :total="pointsTotal"
          :page-size="5"
          :current-page="pointsPage + 1"
          @current-change="(p) => { pointsPage = p - 1; loadPoints() }"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.back-link {
  display: inline-block;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta {
  font-size: 14px;
  color: #666;
  margin: 4px 0;
}

.pagination {
  margin-top: 8px;
  justify-content: flex-end;
}
</style>
