<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryTree } from '../api/category'
import { listProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import { flattenCategories } from '../utils/categoryTree'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  minPrice: route.query.minPrice ? Number(route.query.minPrice) : null,
  maxPrice: route.query.maxPrice ? Number(route.query.maxPrice) : null,
  sort: route.query.sort || 'createdAt,desc',
  page: route.query.page ? Number(route.query.page) : 0,
})

const flatCategories = computed(() => flattenCategories(categories.value))

async function loadCategories() {
  categories.value = await getCategoryTree()
}

async function loadProducts() {
  loading.value = true
  try {
    const data = await listProducts({
      categoryId: filters.categoryId || undefined,
      minPrice: filters.minPrice ?? undefined,
      maxPrice: filters.maxPrice ?? undefined,
      sort: filters.sort,
      page: filters.page,
      size: 12,
    })
    products.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function syncQuery() {
  router.replace({
    query: {
      ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
      ...(filters.minPrice != null ? { minPrice: filters.minPrice } : {}),
      ...(filters.maxPrice != null ? { maxPrice: filters.maxPrice } : {}),
      sort: filters.sort,
      page: filters.page,
    },
  })
}

function selectCategory(categoryId) {
  filters.categoryId = filters.categoryId === categoryId ? null : categoryId
  filters.page = 0
}

function applyPriceRange() {
  filters.page = 0
}

function handlePageChange(page) {
  filters.page = page - 1
}

watch(filters, () => {
  syncQuery()
  loadProducts()
})

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<template>
  <div class="page-container">
    <div class="layout">
      <aside class="sidebar">
        <h4>商品分類</h4>
        <ul class="category-list">
          <li
            v-for="category in flatCategories"
            :key="category.id"
            :class="{ active: filters.categoryId === category.id }"
            :style="{ paddingLeft: `${12 + category.depth * 14}px` }"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </li>
        </ul>

        <h4>價格區間</h4>
        <div class="price-range">
          <el-input-number v-model="filters.minPrice" :min="0" :controls="false" placeholder="最低" size="small" />
          <span>-</span>
          <el-input-number v-model="filters.maxPrice" :min="0" :controls="false" placeholder="最高" size="small" />
        </div>
        <el-button size="small" class="apply-btn" @click="applyPriceRange">套用</el-button>
      </aside>

      <div class="main">
        <div class="toolbar">
          <span class="total-text">共 {{ total }} 件商品</span>
          <el-select v-model="filters.sort" size="small" style="width: 160px">
            <el-option label="最新上架" value="createdAt,desc" />
            <el-option label="價格由低到高" value="price,asc" />
            <el-option label="價格由高到低" value="price,desc" />
            <el-option label="熱銷優先" value="salesCount,desc" />
          </el-select>
        </div>

        <div v-loading="loading" class="product-grid">
          <ProductCard v-for="product in products" :key="product.id" :product="product" />
        </div>
        <el-empty v-if="!loading && products.length === 0" description="找不到符合條件的商品" />

        <el-pagination
          v-if="total > 0"
          class="pagination"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="12"
          :current-page="filters.page + 1"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}

.sidebar h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.category-list {
  list-style: none;
  margin: 0 0 20px;
  padding: 0;
}

.category-list li {
  padding: 6px 12px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
}

.category-list li:hover {
  background: #f5f5f5;
}

.category-list li.active {
  color: #e4393c;
  background: #fdf0f0;
  font-weight: 600;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-range :deep(.el-input-number) {
  width: 80px;
}

.apply-btn {
  margin-top: 10px;
  width: 100%;
}

.main {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.total-text {
  font-size: 13px;
  color: #666;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.pagination {
  margin-top: 24px;
  justify-content: center;
}
</style>
