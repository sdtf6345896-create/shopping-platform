<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listLowStock, updateSkuStock } from '../../api/admin/product'

const router = useRouter()
const loading = ref(false)
const threshold = ref(10)
const items = ref([])
// 每列正在編輯的補貨後庫存(以 skuId 為 key)
const drafts = reactive({})
const saving = reactive({})

async function load() {
  loading.value = true
  try {
    items.value = await listLowStock({ threshold: threshold.value })
    items.value.forEach((item) => {
      drafts[item.skuId] = item.stock
    })
  } finally {
    loading.value = false
  }
}

async function saveStock(row) {
  saving[row.skuId] = true
  try {
    await updateSkuStock(row.productId, row.skuId, drafts[row.skuId])
    ElMessage.success(`已更新「${row.productName} ${row.specName}」庫存`)
    await load()
  } finally {
    saving[row.skuId] = false
  }
}

onMounted(load)
</script>

<template>
  <div class="block">
    <div class="block-title">
      <span>
        庫存警示
        <el-tag v-if="items.length" type="danger" size="small" round>{{ items.length }}</el-tag>
      </span>
      <span class="threshold">
        庫存 ≤
        <el-input-number v-model="threshold" :min="0" :max="9999" size="small" @change="load" />
        件
      </span>
    </div>

    <el-table v-loading="loading" :data="items" size="small">
      <el-table-column prop="productName" label="商品" min-width="180">
        <template #default="{ row }">
          <el-link
            type="primary"
            :underline="false"
            @click="router.push({ name: 'AdminProductEdit', params: { id: row.productId } })"
          >
            {{ row.productName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="specName" label="規格" width="120" />
      <el-table-column prop="skuCode" label="SKU" width="150" />
      <el-table-column label="目前庫存" width="90">
        <template #default="{ row }">
          <span :class="{ 'out-of-stock': row.stock === 0 }">{{ row.stock === 0 ? '缺貨' : row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="補貨後庫存" width="220">
        <template #default="{ row }">
          <div class="restock">
            <el-input-number v-model="drafts[row.skuId]" :min="0" :max="99999" size="small" />
            <el-button
              size="small"
              type="primary"
              :loading="saving[row.skuId]"
              :disabled="drafts[row.skuId] === row.stock"
              @click="saveStock(row)"
            >
              儲存
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && items.length === 0" description="目前沒有低庫存的上架商品" :image-size="60" />
  </div>
</template>

<style scoped>
.block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.threshold {
  font-weight: normal;
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}

.out-of-stock {
  color: #e4393c;
  font-weight: 600;
}

.restock {
  display: flex;
  gap: 6px;
  align-items: center;
}
</style>
