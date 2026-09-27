<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderDetail } from '../api/order'
import { getAdminOrder } from '../api/admin/order'
import PrintableOrder from '../components/PrintableOrder.vue'

const props = defineProps({
  id: { type: [String, Number], required: true },
  // receipt(會員收據)或 packing(後台揀貨單)
  variant: { type: String, default: 'receipt' },
})

const router = useRouter()
const order = ref(null)
const loading = ref(true)

function print() {
  window.print()
}

onMounted(async () => {
  try {
    order.value = props.variant === 'packing' ? await getAdminOrder(props.id) : await getOrderDetail(props.id)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="print-page">
    <div class="toolbar no-print">
      <el-button @click="router.back()">← 返回</el-button>
      <el-button type="primary" :disabled="!order" @click="print">列印 / 另存 PDF</el-button>
    </div>
    <PrintableOrder v-if="order" :order="order" :variant="variant" />
  </div>
</template>

<style scoped>
.print-page {
  padding: 24px 16px;
  min-height: 60vh;
}

.toolbar {
  max-width: 760px;
  margin: 0 auto 16px;
  display: flex;
  justify-content: space-between;
}
</style>

<style>
/* 列印時只留單據本身:隱藏導覽列與操作按鈕 */
@media print {
  .navbar,
  .no-print {
    display: none !important;
  }

  body {
    background: #fff !important;
  }
}
</style>
