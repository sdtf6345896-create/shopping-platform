<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWishlist, removeFromWishlist } from '../../api/wishlist'

const router = useRouter()
const items = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({ page: 0 })

async function load() {
  loading.value = true
  try {
    const data = await listWishlist({ page: filters.page, size: 12 })
    items.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(`確定要將「${item.productName}」移出收藏嗎?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await removeFromWishlist(item.productId)
  ElMessage.success('已移除收藏')
  await load()
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <h3>我的收藏</h3>

    <el-empty v-if="!loading && items.length === 0" description="尚無收藏商品">
      <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
    </el-empty>

    <div v-else class="wishlist-grid">
      <div v-for="item in items" :key="item.id" class="wishlist-card">
        <div class="thumb-wrap" @click="router.push(`/products/${item.productId}`)">
          <img v-if="item.mainImage" :src="item.mainImage" class="thumb" />
          <div v-else class="thumb placeholder">無圖片</div>
          <el-tag v-if="item.productStatus !== 'ON_SALE'" type="info" size="small" class="status-tag">
            已下架
          </el-tag>
        </div>
        <p class="p-name" @click="router.push(`/products/${item.productId}`)">{{ item.productName }}</p>
        <p class="p-price">NT$ {{ item.price }}</p>
        <el-button link type="danger" size="small" @click="handleRemove(item)">移除收藏</el-button>
      </div>
    </div>

    <el-pagination
      v-if="total > 12"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="12"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
h3 {
  margin: 0 0 20px;
}

.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 20px;
}

.wishlist-card {
  display: flex;
  flex-direction: column;
}

.thumb-wrap {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  cursor: pointer;
  margin-bottom: 8px;
}

.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 13px;
}

.status-tag {
  position: absolute;
  top: 6px;
  left: 6px;
}

.p-name {
  margin: 0 0 4px;
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.p-name:hover {
  color: #e4393c;
}

.p-price {
  margin: 0 0 8px;
  color: #e4393c;
  font-weight: 600;
}

.pagination {
  margin-top: 24px;
  justify-content: center;
}
</style>
