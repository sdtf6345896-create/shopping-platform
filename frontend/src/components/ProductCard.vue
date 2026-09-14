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
      <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" />
      <div v-else class="thumb-placeholder">無圖片</div>
    </div>
    <div class="info">
      <p class="name">{{ product.name }}</p>
      <p class="price">NT$ {{ product.price }}</p>
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

.price {
  margin: 0;
  color: #e4393c;
  font-weight: 600;
}
</style>
