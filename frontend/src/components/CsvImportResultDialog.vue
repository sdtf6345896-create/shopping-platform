<script setup>
// 後台 CSV 匯入(全有或全無)的結果視窗:成功顯示筆數,失敗列出每一行的問題
defineProps({
  result: { type: Object, default: null },
  title: { type: String, required: true },
  successText: { type: String, required: true },
  // 失敗時「未 ___」的內容,例如「更新任何庫存」
  rejectedText: { type: String, required: true },
})
defineEmits(['close'])
</script>

<template>
  <el-dialog :model-value="!!result" :title="title" width="520px" @close="$emit('close')">
    <template v-if="result">
      <el-alert v-if="result.applied" type="success" :closable="false" :title="successText" />
      <template v-else>
        <el-alert
          type="error"
          :closable="false"
          :title="`共 ${result.totalRows} 筆資料,有 ${result.errors.length} 個錯誤,未${rejectedText}`"
          description="請修正後重新上傳整份檔案"
        />
        <el-table :data="result.errors" size="small" max-height="300" class="import-errors">
          <el-table-column prop="line" label="行號" width="70" />
          <el-table-column prop="message" label="問題" />
        </el-table>
      </template>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-errors {
  margin-top: 12px;
}
</style>
