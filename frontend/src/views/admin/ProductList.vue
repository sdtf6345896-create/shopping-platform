<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminProducts, updateProductStatus, deleteProduct } from '../../api/admin/product'
import { listAdminCategories } from '../../api/admin/category'
import { flattenCategories } from '../../utils/categoryTree'

const router = useRouter()
const products = ref([])
const total = ref(0)
const loading = ref(true)
const categories = ref([])

const filters = reactive({
  categoryId: null,
  status: null,
  keyword: '',
  page: 0,
})

const flatCategories = computed(() => flattenCategories(categories.value))

async function loadCategories() {
  categories.value = await listAdminCategories()
}

async function loadProducts() {
  loading.value = true
  try {
    const data = await listAdminProducts({
      categoryId: filters.categoryId || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      page: filters.page,
      size: 10,
    })
    products.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 0
  loadProducts()
}

function handlePageChange(page) {
  filters.page = page - 1
  loadProducts()
}

async function handleToggleStatus(row) {
  const nextStatus = row.status === 'ON_SALE' ? 'OFF_SHELF' : 'ON_SALE'
  await updateProductStatus(row.id, nextStatus)
  ElMessage.success(nextStatus === 'ON_SALE' ? '已上架' : '已下架')
  await loadProducts()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`確定要刪除「${row.name}」嗎?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteProduct(row.id)
  ElMessage.success('刪除成功')
  await loadProducts()
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<template>
  <div>
    <div class="header-row">
      <h3>商品管理</h3>
      <el-button type="primary" @click="router.push({ name: 'AdminProductCreate' })">新增商品</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.categoryId" placeholder="全部分類" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="全部狀態" clearable style="width: 140px" @change="handleSearch">
        <el-option label="上架中" value="ON_SALE" />
        <el-option label="已下架" value="OFF_SHELF" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜尋商品名稱"
        style="width: 220px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button @click="handleSearch">搜尋</el-button>
    </div>

    <el-table v-loading="loading" :data="products" class="table">
      <el-table-column label="縮圖" width="80">
        <template #default="{ row }">
          <img v-if="row.mainImage" :src="row.mainImage" class="thumb" />
          <div v-else class="thumb placeholder">無</div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名稱" min-width="200" />
      <el-table-column label="價格" width="100">
        <template #default="{ row }">NT$ {{ row.price }}</template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'">
            {{ row.status === 'ON_SALE' ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="salesCount" label="銷量" width="80" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link size="small" @click="router.push({ name: 'AdminProductEdit', params: { id: row.id } })">
            編輯
          </el-button>
          <el-button link size="small" @click="handleToggleStatus(row)">
            {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
          </el-button>
          <el-button link size="small" type="danger" @click="handleDelete(row)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.table {
  background: #fff;
}

.thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  background: #f5f5f5;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
