<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { getProductDetail, listBoughtTogether, listRelatedProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import ReviewPhotos from '../components/ReviewPhotos.vue'
import { uploadMemberImage } from '../api/upload'
import { formatCountdown, remainingMs } from '../utils/countdown'
import ProductQuestions from '../components/ProductQuestions.vue'
import { listReviews, getReviewSummary, getMyReview, upsertMyReview, deleteMyReview } from '../api/review'
import { isFavorited as fetchIsFavorited, addToWishlist, removeFromWishlist } from '../api/wishlist'
import { recordView } from '../api/browsingHistory'
import { listMyStockAlerts, subscribeStockAlert, unsubscribeStockAlert } from '../api/stockAlert'
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

// 主圖 + 圖庫,去除重複與空值;點縮圖切換大圖
const galleryImages = computed(() => {
  if (!product.value) return []
  return [...new Set([product.value.mainImage, ...(product.value.images || [])].filter(Boolean))]
})
const activeImage = ref(null)
const displayedImage = computed(() => activeImage.value || galleryImages.value[0])

// 限時特價:以選取規格的價格為準;倒數到活動結束
const listPrice = computed(() => selectedSku.value?.price ?? product.value?.price)
const salePrice = computed(() =>
  selectedSku.value ? selectedSku.value.salePrice : product.value?.salePrice,
)
const nowTick = ref(Date.now())
const saleTimer = setInterval(() => {
  nowTick.value = Date.now()
}, 1000)
onUnmounted(() => clearInterval(saleTimer))
const saleTimeLeft = computed(() =>
  product.value?.saleEndAt && salePrice.value != null ? remainingMs(product.value.saleEndAt, nowTick.value) : 0,
)

const selectedSku = computed(() => product.value?.skus.find((s) => s.id === selectedSkuId.value))
const isSoldOut = computed(() => product.value?.status !== 'ON_SALE' || product.value?.skus.every((s) => s.stock === 0))

async function load() {
  loading.value = true
  try {
    try {
      product.value = await getProductDetail(props.id)
    } catch (error) {
      // 商品不存在或已下架:導向 404 頁,保留原網址方便使用者確認
      if (error.response?.status === 404) {
        router.replace({ name: 'NotFound', params: { pathMatch: router.currentRoute.value.path.slice(1).split('/') } })
      }
      throw error
    }
    activeImage.value = null
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

// 貨到通知:選到缺貨規格時可訂閱
const alertSkuIds = ref([])
const togglingAlert = ref(false)
const alertSubscribed = computed(() => alertSkuIds.value.includes(selectedSkuId.value))

async function loadStockAlerts() {
  alertSkuIds.value = authStore.isLoggedIn ? await listMyStockAlerts(props.id) : []
}

async function handleToggleStockAlert() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('請先登入')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  togglingAlert.value = true
  try {
    const skuId = selectedSkuId.value
    if (alertSubscribed.value) {
      await unsubscribeStockAlert(skuId)
      alertSkuIds.value = alertSkuIds.value.filter((id) => id !== skuId)
      ElMessage.success('已取消貨到通知')
    } else {
      await subscribeStockAlert(skuId)
      alertSkuIds.value = [...alertSkuIds.value, skuId]
      ElMessage.success('補貨時會通知你')
    }
  } finally {
    togglingAlert.value = false
  }
}

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
const reviewForm = reactive({ rating: 5, content: '', images: [] })
const MAX_REVIEW_PHOTOS = 5
const uploadingPhoto = ref(false)
const photosOnly = ref(false)

function beforePhotoUpload(file) {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('僅支援 JPG / PNG / WEBP 格式的圖片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('圖片大小請控制在 5MB 以內')
    return false
  }
  if (reviewForm.images.length >= MAX_REVIEW_PHOTOS) {
    ElMessage.warning(`最多 ${MAX_REVIEW_PHOTOS} 張照片`)
    return false
  }
  return true
}

async function handlePhotoUpload({ file }) {
  uploadingPhoto.value = true
  try {
    const data = await uploadMemberImage(file)
    reviewForm.images.push(data.url)
  } finally {
    uploadingPhoto.value = false
  }
}

function removePhoto(index) {
  reviewForm.images.splice(index, 1)
}

function handlePhotosOnlyChange() {
  reviewsPage.value = 0
  loadReviews()
}

async function loadReviewSummary() {
  reviewSummary.value = await getReviewSummary(props.id)
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const data = await listReviews(props.id, {
      page: reviewsPage.value,
      size: 5,
      withImages: photosOnly.value || undefined,
    })
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
  reviewForm.images = [...(myReview.value?.images ?? [])]
  showReviewForm.value = true
}

async function handleSubmitReview() {
  savingReview.value = true
  try {
    await upsertMyReview(props.id, {
      rating: reviewForm.rating,
      content: reviewForm.content,
      images: reviewForm.images,
    })
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

const relatedProducts = ref([])

const boughtTogether = ref([])

async function loadBoughtTogether() {
  try {
    boughtTogether.value = await listBoughtTogether(props.id, { limit: 6 })
  } catch {
    boughtTogether.value = []
  }
}

async function loadRelated() {
  try {
    relatedProducts.value = await listRelatedProducts(props.id, { limit: 6 })
  } catch {
    relatedProducts.value = []
  }
}

async function loadPage() {
  reviewsPage.value = 0
  quantity.value = 1
  await load()
  await Promise.all([loadReviewSummary(), loadReviews(), loadMyReview(), loadFavoriteState(), loadRelated(), loadStockAlerts(), loadBoughtTogether()])

  if (authStore.isLoggedIn) {
    recordView(props.id).catch(() => {
      // 記錄瀏覽紀錄失敗不影響商品頁瀏覽
    })
  }
}

onMounted(loadPage)

// 從相關商品點進另一個商品時,路由元件會被重用,需要手動重新載入
watch(
  () => props.id,
  () => {
    window.scrollTo({ top: 0 })
    loadPage()
  },
)
</script>

<template>
  <div v-loading="loading" class="page-container">
    <template v-if="product">
      <div class="detail-layout">
        <div class="gallery-column">
          <div class="gallery">
            <img v-if="displayedImage" :src="displayedImage" :alt="product.name" />
            <div v-else class="gallery-placeholder">無圖片</div>
          </div>
          <div v-if="galleryImages.length > 1" class="thumbs">
            <button
              v-for="url in galleryImages"
              :key="url"
              type="button"
              class="thumb"
              :class="{ active: url === displayedImage }"
              @click="activeImage = url"
              @mouseenter="activeImage = url"
            >
              <img :src="url" :alt="product.name" />
            </button>
          </div>
        </div>

        <div class="info">
          <h1 class="name">{{ product.name }}</h1>
          <p class="category">分類:{{ product.categoryName }}</p>
          <div v-if="salePrice != null" class="sale-banner">
            <span class="sale-tag">限時特價 -{{ product.saleDiscountPercent }}%</span>
            <span v-if="saleTimeLeft > 0" class="sale-countdown">剩餘 {{ formatCountdown(saleTimeLeft) }}</span>
          </div>
          <p class="price">
            NT$ {{ salePrice ?? listPrice }}
            <span v-if="salePrice != null" class="original-price">NT$ {{ listPrice }}</span>
          </p>

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
            <el-button
              v-if="product.status === 'ON_SALE' && selectedSku?.stock === 0"
              size="large"
              :type="alertSubscribed ? 'info' : 'warning'"
              plain
              :loading="togglingAlert"
              @click="handleToggleStockAlert"
            >
              {{ alertSubscribed ? '已設定貨到通知(取消)' : '貨到通知我' }}
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
              <ReviewPhotos :images="myReview.images" />
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
            <div v-loading="uploadingPhoto" class="review-photo-editor">
              <div v-for="(url, index) in reviewForm.images" :key="url" class="review-photo-item">
                <img :src="url" alt="評論照片" />
                <button type="button" class="remove-photo" @click="removePhoto(index)">×</button>
              </div>
              <el-upload
                v-if="reviewForm.images.length < MAX_REVIEW_PHOTOS"
                class="review-photo-uploader"
                multiple
                :show-file-list="false"
                :before-upload="beforePhotoUpload"
                :http-request="handlePhotoUpload"
                accept="image/jpeg,image/png,image/webp"
              >
                <div class="review-photo-add">+ 照片</div>
              </el-upload>
            </div>
            <div class="review-form-actions">
              <el-button size="small" @click="showReviewForm = false">取消</el-button>
              <el-button size="small" type="primary" :loading="savingReview" @click="handleSubmitReview">
                送出
              </el-button>
            </div>
          </div>
        </div>

        <el-checkbox v-model="photosOnly" class="photos-only" @change="handlePhotosOnlyChange">
          只看有照片的評論
        </el-checkbox>

        <div v-loading="reviewsLoading" class="review-list">
          <el-empty v-if="!reviewsLoading && reviews.length === 0" description="還沒有人評論,搶頭香吧" :image-size="60" />
          <div v-for="review in reviews" :key="review.id" class="review-card">
            <div class="review-card-header">
              <el-rate :model-value="review.rating" disabled />
              <span class="review-author">{{ review.memberName }}</span>
              <span class="review-date">{{ review.createdAt?.slice(0, 10) }}</span>
            </div>
            <p class="review-content">{{ review.content || '(未留言)' }}</p>
            <ReviewPhotos :images="review.images" />
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

      <ProductQuestions :product-id="props.id" />

      <div v-if="boughtTogether.length" class="related-section">
        <h2 class="related-title">買了這個的人也買了</h2>
        <div class="related-grid">
          <div v-for="entry in boughtTogether" :key="entry.product.id">
            <ProductCard :product="entry.product" />
            <p class="together-count">{{ entry.orderCount }} 筆訂單一起購買</p>
          </div>
        </div>
      </div>

      <div v-if="relatedProducts.length" class="related-section">
        <h2 class="related-title">你可能也會喜歡</h2>
        <div class="related-grid">
          <ProductCard v-for="item in relatedProducts" :key="item.id" :product="item" />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.related-section {
  margin-top: 24px;
}

.together-count {
  margin: 4px 0 0;
  color: #999;
  font-size: 12px;
  text-align: center;
}

.related-title {
  font-size: 18px;
  margin-bottom: 12px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.detail-layout {
  display: flex;
  gap: 32px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
}

.gallery-column {
  width: 400px;
  flex-shrink: 0;
}

.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.thumb {
  width: 64px;
  height: 64px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 4px;
  background: #f5f5f5;
  cursor: pointer;
  overflow: hidden;
}

.thumb.active {
  border-color: #e4393c;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.gallery {
  width: 400px;
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

.original-price {
  margin-left: 8px;
  color: #999;
  font-size: 15px;
  font-weight: normal;
  text-decoration: line-through;
}

.sale-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.sale-tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: #e4393c;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.sale-countdown {
  color: #e4393c;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
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

.photos-only {
  margin: 8px 0;
}

.review-photo-editor {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.review-photo-item {
  position: relative;
  width: 64px;
  height: 64px;
}

.review-photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.remove-photo {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  cursor: pointer;
}

.review-photo-add {
  width: 64px;
  height: 64px;
  border: 1px dashed #ccc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
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
