<script setup>
// 待回覆的訂單留言:每張訂單最後一則是買家留言的,舊的排前面
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listAwaitingOrderMessages } from '../../api/admin/order'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(true)

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

async function load() {
  loading.value = true
  try {
    const data = await listAwaitingOrderMessages({ page: page.value, size: 20 })
    rows.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h3>待回覆訂單留言</h3>
    <p class="hint">回覆後會以站內通知提醒買家;買家再留言時會重新出現在這裡。</p>
    <el-table v-loading="loading" :data="rows" empty-text="目前沒有待回覆的留言 🎉">
      <el-table-column label="訂單編號" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push({ name: 'AdminOrderDetail', params: { id: row.orderId } })">
            {{ row.orderNo }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="買家" width="200">
        <template #default="{ row }">
          <div>{{ row.memberName }}</div>
          <small class="hint">{{ row.memberEmail }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="lastMessage" label="最新留言" show-overflow-tooltip />
      <el-table-column label="留言時間" width="150">
        <template #default="{ row }">{{ formatTime(row.lastMessageAt) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 20"
      layout="prev, pager, next"
      :total="total"
      :page-size="20"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.hint {
  color: #999;
  font-size: 12px;
}
</style>
