<script setup>
defineProps({
  product: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <router-link :to="`/products/${product.id}`" class="product-card">
    <div class="thumb">
      <span v-if="product.salePrice != null" class="sale-badge">-{{ product.saleDiscountPercent }}%</span>
      <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
      <div v-else class="thumb-placeholder">無圖片</div>
    </div>
    <div class="info">
      <p class="name">{{ product.name }}</p>
      <div class="meta-row">
        <p class="price">
          NT$ {{ product.salePrice ?? product.price }}
          <span v-if="product.salePrice != null" class="original-price">NT$ {{ product.price }}</span>
        </p>
        <span v-if="product.reviewCount > 0" class="rating" :title="`${product.reviewCount} 則評論`">
          ★ {{ Number(product.ratingAverage).toFixed(1) }}
          <span class="rating-count">({{ product.reviewCount }})</span>
        </span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.product-card {
  display: block;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  color: #333;
  transition: box-shadow 0.2s;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.thumb {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  color: #bbb;
  font-size: 13px;
}

.info {
  padding: 10px 12px;
}

.name {
  margin: 0 0 6px;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 38px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 6px;
}

.rating {
  color: #f7ba2a;
  font-size: 12px;
  white-space: nowrap;
}

.rating-count {
  color: #999;
}

.sale-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #e4393c;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.original-price {
  margin-left: 4px;
  color: #999;
  font-size: 12px;
  font-weight: normal;
  text-decoration: line-through;
}

.price {
  margin: 0;
  color: #e4393c;
  font-weight: 600;
}
</style>
