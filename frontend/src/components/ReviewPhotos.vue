<script setup>
defineProps({
  images: { type: Array, default: () => [] },
})
</script>

<template>
  <div v-if="images.length" class="review-photos">
    <el-image
      v-for="(url, index) in images"
      :key="url"
      :src="url"
      :preview-src-list="images"
      :initial-index="index"
      fit="cover"
      class="photo"
      preview-teleported
      lazy
    />
  </div>
</template>

<style scoped>
.review-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.photo {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  cursor: zoom-in;
}
</style>
