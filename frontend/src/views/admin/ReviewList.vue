<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminReviews, replyReview, setReviewHidden } from '../../api/admin/review'
import ReviewPhotos from '../../components/ReviewPhotos.vue'

const reviews = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  keyword: '',
  rating: null,
  replied: null, // null | true | false
  hidden: null,
  page: 0,
})

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

async function load() {
  loading.value = true
  try {
    const data = await listAdminReviews({
      keyword: filters.keyword.trim() || undefined,
      rating: filters.rating ?? undefined,
      replied: filters.replied ?? undefined,
      hidden: filters.hidden ?? undefined,
      page: filters.page,
      size: 10,
    })
    reviews.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

async function handleReply(row) {
  let value
  try {
    const result = await ElMessageBox.prompt(
      `回覆 ${row.memberName} 對「${row.productName}」的評價(會公開顯示在商品頁,留空則刪除回覆)`,
      row.sellerReply ? '編輯回覆' : '回覆評價',
      {
        inputType: 'textarea',
        inputValue: row.sellerReply || '',
        inputValidator: (v) => (v || '').length <= 500 || '回覆最多 500 字',
        confirmButtonText: '儲存',
      },
    )
    value = result.value
  } catch {
    return
  }
  const updated = await replyReview(row.id, value || '')
  Object.assign(row, updated)
  ElMessage.success(updated.sellerReply ? '已儲存回覆' : '已刪除回覆')
}

async function handleToggleHidden(row) {
  const hide = !row.hidden
  if (hide) {
    try {
      await ElMessageBox.confirm('隱藏後前台看不到這則評價,也不會列入商品評分。確定隱藏?', '隱藏評價', {
        type: 'warning',
      })
    } catch {
      return
    }
  }
  const updated = await setReviewHidden(row.id, hide)
  Object.assign(row, updated)
  ElMessage.success(hide ? '已隱藏' : '已取消隱藏')
}

onMounted(load)
</script>

<template>
  <div>
    <h3>評價管理</h3>

    <div class="filter-row">
      <el-input
        v-model="filters.keyword"
        placeholder="商品名稱"
        clearable
        style="width: 200px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="filters.rating" placeholder="全部星等" clearable style="width: 120px" @change="search">
        <el-option v-for="n in [5, 4, 3, 2, 1]" :key="n" :label="`${n} 星`" :value="n" />
      </el-select>
      <el-select v-model="filters.replied" placeholder="回覆狀態" clearable style="width: 120px" @change="search">
        <el-option label="未回覆" :value="false" />
        <el-option label="已回覆" :value="true" />
      </el-select>
      <el-select v-model="filters.hidden" placeholder="顯示狀態" clearable style="width: 120px" @change="search">
        <el-option label="顯示中" :value="false" />
        <el-option label="已隱藏" :value="true" />
      </el-select>
      <el-button type="primary" @click="search">搜尋</el-button>
    </div>

    <el-table v-loading="loading" :data="reviews" empty-text="沒有符合條件的評價">
      <el-table-column label="商品" width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.productName }}</template>
      </el-table-column>
      <el-table-column label="會員" width="150">
        <template #default="{ row }">
          <div>{{ row.memberName }}</div>
          <small class="muted">{{ formatTime(row.createdAt) }}</small>
        </template>
      </el-table-column>
      <el-table-column label="評價">
        <template #default="{ row }">
          <el-rate :model-value="row.rating" disabled size="small" />
          <p class="content" :class="{ hidden: row.hidden }">{{ row.content || '(未留言)' }}</p>
          <ReviewPhotos :images="row.images" />
          <div v-if="row.sellerReply" class="reply">
            <strong>賣家回覆:</strong>{{ row.sellerReply }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.hidden" type="info" size="small">已隱藏</el-tag>
          <el-tag v-else-if="!row.sellerReply" type="warning" size="small">未回覆</el-tag>
          <el-tag v-else type="success" size="small">已回覆</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="handleReply(row)">
            {{ row.sellerReply ? '編輯回覆' : '回覆' }}
          </el-button>
          <el-button link size="small" :type="row.hidden ? 'success' : 'danger'" @click="handleToggleHidden(row)">
            {{ row.hidden ? '取消隱藏' : '隱藏' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 10"
      class="pagination"
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.muted {
  color: #999;
}

.content {
  margin: 4px 0;
  white-space: pre-wrap;
}

.content.hidden {
  color: #bbb;
  text-decoration: line-through;
}

.reply {
  margin-top: 6px;
  padding: 6px 10px;
  background: #f7f8fa;
  border-radius: 4px;
  font-size: 13px;
  white-space: pre-wrap;
}

.pagination {
  margin-top: 16px;
}
</style>
