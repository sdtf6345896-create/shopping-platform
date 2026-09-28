<script setup>
// 商品比較:最多 4 件並排比較價格、評分、庫存與規格表
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getProductDetail } from '../api/product'
import { useCompareStore } from '../stores/compare'
import { buildSpecRows } from '../utils/compareTable'

const router = useRouter()
const compareStore = useCompareStore()
const products = ref([])
const loading = ref(false)

const specRows = computed(() => buildSpecRows(products.value))

function priceOf(product) {
  return product.salePrice ?? product.price
}

function totalStock(product) {
  return product.skus.reduce((sum, sku) => sum + sku.stock, 0)
}

async function load() {
  loading.value = true
  try {
    const ids = [...compareStore.ids]
    const results = await Promise.allSettled(ids.map((id) => getProductDetail(id)))
    // 已下架或刪除的商品(404)自動從比較清單移除(用快照的 id,移除時清單長度會變)
    results.forEach((r, i) => {
      if (r.status === 'rejected') compareStore.remove(ids[i])
    })
    products.value = results.filter((r) => r.status === 'fulfilled').map((r) => r.value)
  } finally {
    loading.value = false
  }
}

function remove(id) {
  compareStore.remove(id)
  products.value = products.value.filter((p) => p.id !== id)
}

watch(() => compareStore.ids.join(','), load, { immediate: true })
</script>

<template>
  <div v-loading="loading" class="page-container compare-page">
    <div class="header-row">
      <h2>商品比較</h2>
      <el-button v-if="products.length" link type="danger" @click="compareStore.clear()">清空比較</el-button>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="還沒有加入比較的商品">
      <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
    </el-empty>

    <div v-else class="table-scroll">
      <table class="compare-table">
        <tr>
          <th></th>
          <td v-for="p in products" :key="p.id" class="product-cell">
            <img :src="p.mainImage || '/favicon.svg'" :alt="p.name" @click="router.push(`/products/${p.id}`)" />
            <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
            <el-button link size="small" type="danger" @click="remove(p.id)">移除</el-button>
          </td>
        </tr>
        <tr>
          <th>價格</th>
          <td v-for="p in products" :key="p.id">
            <span class="price">NT$ {{ priceOf(p) }}</span>
            <span v-if="p.salePrice != null" class="original">NT$ {{ p.price }}</span>
          </td>
        </tr>
        <tr>
          <th>評分</th>
          <td v-for="p in products" :key="p.id">
            <template v-if="p.reviewCount > 0">★ {{ p.ratingAverage }}({{ p.reviewCount }} 則)</template>
            <template v-else>尚無評價</template>
          </td>
        </tr>
        <tr>
          <th>銷量</th>
          <td v-for="p in products" :key="p.id">{{ p.salesCount }}</td>
        </tr>
        <tr>
          <th>分類</th>
          <td v-for="p in products" :key="p.id">{{ p.categoryName }}</td>
        </tr>
        <tr>
          <th>可選規格</th>
          <td v-for="p in products" :key="p.id">{{ p.skus.map((s) => s.specName).join('、') }}</td>
        </tr>
        <tr>
          <th>庫存</th>
          <td v-for="p in products" :key="p.id">{{ totalStock(p) > 0 ? '有貨' : '已售完' }}</td>
        </tr>
        <tr v-for="row in specRows" :key="row.name" :class="{ same: row.same }">
          <th>{{ row.name }}</th>
          <td v-for="(value, i) in row.values" :key="i">{{ value }}</td>
        </tr>
      </table>
      <p v-if="products.length < 2" class="hint">再加入至少一件商品,就能並排比較(最多 4 件)。</p>
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-scroll {
  overflow-x: auto;
}

.compare-table {
  border-collapse: collapse;
  min-width: 100%;
  font-size: 14px;
}

.compare-table th,
.compare-table td {
  border: 1px solid #eee;
  padding: 10px 12px;
  vertical-align: top;
  text-align: left;
}

.compare-table th {
  width: 110px;
  background: #fafafa;
  color: #666;
  font-weight: normal;
  white-space: nowrap;
}

.compare-table td {
  min-width: 180px;
}

.product-cell img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  display: block;
  margin-bottom: 6px;
}

.product-cell .name {
  display: block;
  color: #333;
  font-weight: bold;
  margin-bottom: 4px;
}

.price {
  color: #e4393c;
  font-weight: bold;
}

.original {
  margin-left: 6px;
  color: #999;
  text-decoration: line-through;
  font-size: 12px;
}

tr.same td {
  color: #999;
}

.hint {
  color: #999;
  font-size: 13px;
}
</style>
