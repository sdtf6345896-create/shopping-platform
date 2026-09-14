<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(true)
const selectedIds = ref([])

const selectedItems = computed(() => cartStore.items.filter((item) => selectedIds.value.includes(item.id)))
const selectedTotal = computed(() => selectedItems.value.reduce((sum, item) => sum + item.subtotal, 0))
const allSelected = computed(
  () => cartStore.items.length > 0 && selectedIds.value.length === cartStore.items.length,
)

async function load() {
  loading.value = true
  try {
    await cartStore.fetchCart()
    selectedIds.value = cartStore.items.filter((i) => i.productStatus === 'ON_SALE').map((i) => i.id)
  } finally {
    loading.value = false
  }
}

function toggleSelectAll(value) {
  selectedIds.value = value ? cartStore.items.map((i) => i.id) : []
}

async function handleQuantityChange(item, value) {
  try {
    await cartStore.updateQuantity(item.id, value)
  } catch {
    // 更新失敗(例如超過庫存)時只重新整理購物車資料,不動使用者已勾選的項目
    await cartStore.fetchCart()
  }
}

async function handleRemove(item) {
  await cartStore.removeItem(item.id)
  selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
  ElMessage.success('已移除')
}

async function handleClear() {
  await ElMessageBox.confirm('確定要清空購物車嗎?', '提示', { type: 'warning' })
  await cartStore.clear()
  selectedIds.value = []
}

function handleCheckout() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('請選擇要結帳的商品')
    return
  }
  cartStore.setCheckoutSelection(selectedIds.value)
  router.push('/checkout')
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <h2>購物車</h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && cartStore.items.length === 0" description="購物車是空的">
        <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <div class="cart-table">
          <div class="cart-header">
            <el-checkbox :model-value="allSelected" @change="toggleSelectAll">全選</el-checkbox>
            <span>商品</span>
            <span>單價</span>
            <span>數量</span>
            <span>小計</span>
            <span></span>
          </div>

          <div v-for="item in cartStore.items" :key="item.id" class="cart-row">
            <el-checkbox v-model="selectedIds" :value="item.id" />
            <div class="product-cell">
              <img v-if="item.mainImage" :src="item.mainImage" class="thumb" />
              <div>
                <p class="p-name">{{ item.productName }}</p>
                <p class="p-spec">{{ item.specName }}</p>
                <el-tag v-if="item.productStatus !== 'ON_SALE'" type="danger" size="small">已下架,請移除</el-tag>
              </div>
            </div>
            <span>NT$ {{ item.price }}</span>
            <el-input-number
              :model-value="item.quantity"
              :min="1"
              :max="item.stock"
              size="small"
              @change="(v) => handleQuantityChange(item, v)"
            />
            <span class="subtotal">NT$ {{ item.subtotal }}</span>
            <el-button link type="danger" @click="handleRemove(item)">移除</el-button>
          </div>
        </div>

        <div class="cart-footer">
          <el-button @click="handleClear">清空購物車</el-button>
          <div class="summary">
            <span>已選 {{ selectedItems.length }} 項,合計:</span>
            <span class="total-amount">NT$ {{ selectedTotal }}</span>
            <el-button type="primary" size="large" @click="handleCheckout">前往結帳</el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cart-table {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.cart-header,
.cart-row {
  display: grid;
  grid-template-columns: 40px 2fr 1fr 1fr 1fr 60px;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.cart-header {
  background: #fafafa;
  font-size: 13px;
  color: #999;
}

.cart-row {
  border-top: 1px solid #f0f0f0;
}

.product-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  background: #f5f5f5;
}

.p-name {
  margin: 0 0 4px;
  font-size: 14px;
}

.p-spec {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
}

.subtotal {
  color: #e4393c;
  font-weight: 600;
}

.cart-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-amount {
  color: #e4393c;
  font-size: 20px;
  font-weight: 700;
}
</style>
