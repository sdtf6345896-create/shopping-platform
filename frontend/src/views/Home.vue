<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '../api/category'
import { listProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const categories = ref([])
const hotProducts = ref([])
const loading = ref(true)

const banners = [
  { title: '新品上市', subtitle: '本季新款搶先看', gradient: 'linear-gradient(135deg,#ff6b6b,#e4393c)' },
  { title: '限時優惠', subtitle: '精選商品折扣中', gradient: 'linear-gradient(135deg,#4facfe,#00f2fe)' },
  { title: '會員專屬', subtitle: '註冊即享購物優惠', gradient: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
]

onMounted(async () => {
  try {
    const [categoryData, productData] = await Promise.all([
      getCategoryTree(),
      listProducts({ sort: 'salesCount,desc', size: 8 }),
    ])
    categories.value = categoryData
    hotProducts.value = productData.content
  } finally {
    loading.value = false
  }
})

function goToCategory(categoryId) {
  router.push({ path: '/products', query: { categoryId } })
}
</script>

<template>
  <div class="page-container">
    <el-carousel height="260px" class="banner">
      <el-carousel-item v-for="banner in banners" :key="banner.title">
        <div class="banner-slide" :style="{ background: banner.gradient }">
          <h2>{{ banner.title }}</h2>
          <p>{{ banner.subtitle }}</p>
        </div>
      </el-carousel-item>
    </el-carousel>

    <section class="section">
      <h3 class="section-title">商品分類</h3>
      <div class="category-row">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-item"
          @click="goToCategory(category.id)"
        >
          {{ category.name }}
        </div>
        <el-empty v-if="!loading && categories.length === 0" description="尚無分類" :image-size="60" />
      </div>
    </section>

    <section class="section">
      <h3 class="section-title">熱銷推薦</h3>
      <div v-loading="loading" class="product-grid">
        <ProductCard v-for="product in hotProducts" :key="product.id" :product="product" />
        <el-empty v-if="!loading && hotProducts.length === 0" description="尚無商品" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.banner-slide {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.banner-slide h2 {
  font-size: 28px;
  margin: 0 0 8px;
}

.banner-slide p {
  margin: 0;
  opacity: 0.9;
}

.section {
  margin-top: 32px;
}

.section-title {
  font-size: 20px;
  margin-bottom: 16px;
  border-left: 4px solid #e4393c;
  padding-left: 10px;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-item {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 14px;
}

.category-item:hover {
  border-color: #e4393c;
  color: #e4393c;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 100px;
}
</style>
