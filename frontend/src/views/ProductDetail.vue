<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '../api/product'
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

onMounted(load)
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

          <div class="description">
            <p class="label">商品描述</p>
            <p>{{ product.description || '暫無商品描述' }}</p>
          </div>
        </div>
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

.add-cart-btn {
  margin-top: 24px;
  width: 220px;
}

.description p {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
