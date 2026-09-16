<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { getProductDetail } from '../api/product'
import { listReviews, getReviewSummary, getMyReview, upsertMyReview, deleteMyReview } from '../api/review'
import { isFavorited as fetchIsFavorited, addToWishlist, removeFromWishlist } from '../api/wishlist'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const product = ref(null)
const loading = ref(true)
const selectedSkuId = ref(null)
const quantity = ref(1)
const submitting = ref(false)

const selectedSku = computed(() => product.value?.skus.find((s) => s.id === selectedSkuId.value))
const isSoldOut = computed(() => product.value?.status !== 'ON_SALE' || product.value?.skus.every((s) => s.stock === 0))

async function load() {
  loading.value = true
  try {
    product.value = await getProductDetail(props.id)
    const availableSku = product.value.skus.find((s) => s.stock > 0) || product.value.skus[0]
    selectedSkuId.value = availableSku?.id ?? null
  } finally {
    loading.value = false
  }
}

function selectSku(sku) {
  selectedSkuId.value = sku.id
  quantity.value = 1
}

async function handleAddToCart() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('請先登入')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (!selectedSku.value || selectedSku.value.stock === 0) {
    ElMessage.warning('此規格已無庫存')
    return
  }
  submitting.value = true
  try {
    await cartStore.addItem(selectedSkuId.value, quantity.value)
    ElMessage.success('已加入購物車')
  } finally {
    submitting.value = false
  }
}

const favorited = ref(false)
const togglingFavorite = ref(false)

async function loadFavoriteState() {
  if (!authStore.isLoggedIn) {
    favorited.value = false
    return
  }
  favorited.value = await fetchIsFavorited(props.id)
}

async function handleToggleFavorite() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('請先登入')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  togglingFavorite.value = true
  try {
    if (favorited.value) {
      await removeFromWishlist(props.id)
      favorited.value = false
      ElMessage.success('已移除收藏')
    } else {
      await addToWishlist(props.id)
      favorited.value = true
      ElMessage.success('已加入收藏')
    }
  } finally {
    togglingFavorite.value = false
  }
}

const reviewSummary = ref({ averageRating: 0, reviewCount: 0 })
const reviews = ref([])
const reviewsTotal = ref(0)
const reviewsPage = ref(0)
const reviewsLoading = ref(true)
const myReview = ref(null)
const showReviewForm = ref(false)
const savingReview = ref(false)
const reviewForm = reactive({ rating: 5, content: '' })

async function loadReviewSummary() {
  reviewSummary.value = await getReviewSummary(props.id)
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const data = await listReviews(props.id, { page: reviewsPage.value, size: 5 })
    reviews.value = data.content
    reviewsTotal.value = data.totalElements
  } finally {
    reviewsLoading.value = false
  }
}

async function loadMyReview() {
  if (!authStore.isLoggedIn) {
    myReview.value = null
    return
  }
  myReview.value = await getMyReview(props.id)
}

function handleReviewPageChange(page) {
  reviewsPage.value = page - 1
  loadReviews()
}

function openReviewForm() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('請先登入')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  reviewForm.rating = myReview.value?.rating ?? 5
  reviewForm.content = myReview.value?.content ?? ''
  showReviewForm.value = true
}

async function handleSubmitReview() {
  savingReview.value = true
  try {
    await upsertMyReview(props.id, { rating: reviewForm.rating, content: reviewForm.content })
    ElMessage.success('評論送出成功')
    showReviewForm.value = false
    await Promise.all([loadReviewSummary(), loadReviews(), loadMyReview()])
  } finally {
    savingReview.value = false
  }
}

async function handleDeleteReview() {
  await ElMessageBox.confirm('確定要刪除你的評論嗎?', '提示', { type: 'warning' })
  await deleteMyReview(props.id)
  ElMessage.success('評論已刪除')
  await Promise.all([loadReviewSummary(), loadReviews(), loadMyReview()])
}

onMounted(async () => {
  await load()
  await Promise.all([loadReviewSummary(), loadReviews(), loadMyReview(), loadFavoriteState()])
})
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="product">
      <div class="detail-layout">
        <div class="gallery">
          <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
          <div v-else class="gallery-placeholder">無圖片</div>
        </div>

        <div class="info">
          <h1 class="name">{{ product.name }}</h1>
          <p class="category">分類:{{ product.categoryName }}</p>
          <p class="price">NT$ {{ selectedSku?.price ?? product.price }}</p>

          <el-tag v-if="product.status !== 'ON_SALE'" type="info">此商品已下架</el-tag>

          <div class="spec-section">
            <p class="label">規格</p>
            <div class="sku-list">
              <el-tag
                v-for="sku in product.skus"
                :key="sku.id"
                :effect="selectedSkuId === sku.id ? 'dark' : 'plain'"
                :type="sku.stock === 0 ? 'info' : 'primary'"
                class="sku-tag"
                :class="{ disabled: sku.stock === 0 }"
                @click="sku.stock > 0 && selectSku(sku)"
              >
                {{ sku.specName }}{{ sku.stock === 0 ? '(缺貨)' : '' }}
              </el-tag>
            </div>
          </div>

          <div class="qty-section">
            <p class="label">數量</p>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="selectedSku?.stock || 1"
              :disabled="!selectedSku || selectedSku.stock === 0"
            />
            <span v-if="selectedSku" class="stock-hint">庫存:{{ selectedSku.stock }}</span>
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              size="large"
              class="add-cart-btn"
              :loading="submitting"
              :disabled="isSoldOut"
              @click="handleAddToCart"
            >
              {{ isSoldOut ? '已售完' : '加入購物車' }}
            </el-button>
            <el-button
              size="large"
              class="favorite-btn"
              :class="{ favorited }"
              :loading="togglingFavorite"
              @click="handleToggleFavorite"
            >
              <el-icon><component :is="favorited ? StarFilled : Star" /></el-icon>
              {{ favorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>

          <div class="description">
            <p class="label">商品描述</p>
            <p>{{ product.description || '暫無商品描述' }}</p>
          </div>
        </div>
      </div>

      <div class="review-section">
        <div class="review-header">
          <h3>商品評論</h3>
          <div class="review-summary">
            <el-rate :model-value="reviewSummary.averageRating" disabled allow-half />
            <span class="summary-text">
              {{ reviewSummary.averageRating }} 分({{ reviewSummary.reviewCount }} 則評論)
            </span>
          </div>
        </div>

        <div class="my-review-block">
          <template v-if="myReview">
            <div class="review-card my-review-card">
              <div class="review-card-header">
                <el-rate :model-value="myReview.rating" disabled />
                <span class="review-author">你的評論</span>
              </div>
              <p class="review-content">{{ myReview.content || '(未留言)' }}</p>
              <div class="review-actions">
                <el-button link size="small" @click="openReviewForm">編輯</el-button>
                <el-button link size="small" type="danger" @click="handleDeleteReview">刪除</el-button>
              </div>
            </div>
          </template>
          <el-button v-else @click="openReviewForm">撰寫評論</el-button>

          <div v-if="showReviewForm" class="review-form">
            <el-rate v-model="reviewForm.rating" />
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="分享你的使用心得(選填)"
            />
            <div class="review-form-actions">
              <el-button size="small" @click="showReviewForm = false">取消</el-button>
              <el-button size="small" type="primary" :loading="savingReview" @click="handleSubmitReview">
                送出
              </el-button>
            </div>
          </div>
        </div>

        <div v-loading="reviewsLoading" class="review-list">
          <el-empty v-if="!reviewsLoading && reviews.length === 0" description="還沒有人評論,搶頭香吧" :image-size="60" />
          <div v-for="review in reviews" :key="review.id" class="review-card">
            <div class="review-card-header">
              <el-rate :model-value="review.rating" disabled />
              <span class="review-author">{{ review.memberName }}</span>
              <span class="review-date">{{ review.createdAt?.slice(0, 10) }}</span>
            </div>
            <p class="review-content">{{ review.content || '(未留言)' }}</p>
          </div>
        </div>

        <el-pagination
          v-if="reviewsTotal > 5"
          class="pagination"
          background
          layout="prev, pager, next"
          :total="reviewsTotal"
          :page-size="5"
          :current-page="reviewsPage + 1"
          @current-change="handleReviewPageChange"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-layout {
  display: flex;
  gap: 32px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
}

.gallery {
  width: 400px;
  flex-shrink: 0;
  aspect-ratio: 1 / 1;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  overflow: hidden;
}

.gallery img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-placeholder {
  color: #bbb;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 22px;
  margin: 0 0 8px;
}

.category {
  color: #999;
  font-size: 13px;
  margin: 0 0 12px;
}

.price {
  color: #e4393c;
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 16px;
}

.label {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px;
}

.spec-section,
.qty-section,
.description {
  margin-top: 20px;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sku-tag {
  cursor: pointer;
}

.sku-tag.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.qty-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-hint {
  font-size: 13px;
  color: #999;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.add-cart-btn {
  width: 220px;
}

.favorite-btn.favorited {
  color: #e6a23c;
  border-color: #e6a23c;
}

.description p {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  white-space: pre-wrap;
}

.review-section {
  margin-top: 20px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.review-header h3 {
  margin: 0;
  font-size: 18px;
}

.review-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-text {
  font-size: 13px;
  color: #666;
}

.my-review-block {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #eee;
}

.review-form {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 480px;
}

.review-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.review-list {
  min-height: 60px;
}

.review-card {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.my-review-card {
  border-bottom: none;
  padding: 0;
}

.review-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.review-author {
  font-size: 13px;
  color: #333;
  font-weight: 600;
}

.review-date {
  font-size: 12px;
  color: #999;
}

.review-content {
  margin: 0;
  font-size: 14px;
  color: #555;
  white-space: pre-wrap;
}

.review-actions {
  margin-top: 6px;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
