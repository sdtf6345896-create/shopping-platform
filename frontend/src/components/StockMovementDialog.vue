<script setup>
// 單一規格的庫存異動紀錄(後台)
import { ref, watch } from 'vue'
import { listStockMovements } from '../api/admin/product'

const props = defineProps({
  productId: { type: [String, Number], default: null },
  // { id, skuCode, specName },null 時關閉
  sku: { type: Object, default: null },
})
const emit = defineEmits(['close'])

const REASON_LABELS = {
  INITIAL: '初始庫存',
  ORDER: '訂單出貨扣庫存',
  ORDER_CANCEL: '訂單取消歸還',
  RETURN: '退貨歸還',
  MANUAL: '手動調整',
  IMPORT: 'CSV 匯入',
  PRODUCT_EDIT: '編輯商品',
}

const movements = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

async function load() {
  loading.value = true
  try {
    const data = await listStockMovements(props.productId, props.sku.id, { page: page.value, size: 10 })
    movements.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

watch(
  () => props.sku,
  (sku) => {
    if (sku) {
      page.value = 0
      load()
    }
  },
)

function handlePageChange(p) {
  page.value = p - 1
  load()
}
</script>

<template>
  <el-dialog
    :model-value="!!sku"
    :title="sku ? `庫存異動紀錄:${sku.specName}(${sku.skuCode})` : ''"
    width="640px"
    @close="emit('close')"
  >
    <el-table v-loading="loading" :data="movements" size="small" empty-text="尚無異動紀錄">
      <el-table-column label="時間" width="140">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="原因" width="130">
        <template #default="{ row }">{{ REASON_LABELS[row.reason] || row.reason }}</template>
      </el-table-column>
      <el-table-column label="異動" width="80" align="right">
        <template #default="{ row }">
          <span :class="row.changeQty > 0 ? 'plus' : 'minus'">
            {{ row.changeQty > 0 ? `+${row.changeQty}` : row.changeQty }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="stockAfter" label="異動後" width="80" align="right" />
      <el-table-column label="備註" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reference || '-' }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 10"
      small
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </el-dialog>
</template>

<style scoped>
.plus {
  color: #67c23a;
}

.minus {
  color: #e4393c;
}
</style>
