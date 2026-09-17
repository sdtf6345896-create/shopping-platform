<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '../api/category'
import { listProducts } from '../api/product'
import { getBanners } from '../api/banner'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const categories = ref([])
const hotProducts = ref([])
const banners = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [categoryData, productData, bannerData] = await Promise.all([
      getCategoryTree(),
      listProducts({ sort: 'salesCount,desc', size: 8 }),
      getBanners(),
    ])
    categories.value = categoryData
    hotProducts.value = productData.content
    banners.value = bannerData
  } finally {
    loading.value = false
  }
})

function goToCategory(categoryId) {
  router.push({ path: '/products', query: { categoryId } })
}

function goToBanner(banner) {
  if (!banner.linkUrl) return
  if (/^https?:\/\//.test(banner.linkUrl)) {
    window.open(banner.linkUrl, '_blank')
  } else {
    router.push(banner.linkUrl)
  }
}
</script>

<template>
  <div class="page-container">
    <el-carousel v-if="banners.length" height="260px" class="banner">
      <el-carousel-item v-for="banner in banners" :key="banner.id">
        <div
          class="banner-slide"
          :class="{ clickable: !!banner.linkUrl }"
          :style="{ backgroundImage: `url(${banner.imageUrl})` }"
          @click="goToBanner(banner)"
        >
          <div class="banner-overlay">
            <h2>{{ banner.title }}</h2>
            <p v-if="banner.subtitle">{{ banner.subtitle }}</p>
          </div>
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
  align-items: flex-end;
  background-size: cover;
  background-position: center;
  color: #fff;
}

.banner-slide.clickable {
  cursor: pointer;
}

.banner-overlay {
  width: 100%;
  padding: 20px 28px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.55), rgba(0, 0, 0, 0));
}

.banner-overlay h2 {
  font-size: 26px;
  margin: 0 0 6px;
}

.banner-overlay p {
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
