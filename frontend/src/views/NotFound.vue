<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="page-container not-found">
    <el-result icon="warning" title="找不到這個頁面" sub-title="網址可能有誤,或商品已下架">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">回首頁</el-button>
        <el-button @click="router.push('/products')">逛逛全部商品</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  padding: 60px 0;
}
</style>
