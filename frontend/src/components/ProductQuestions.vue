<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { askQuestion, listQuestions } from '../api/question'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  productId: { type: [String, Number], required: true },
})

const PAGE_SIZE = 5

const router = useRouter()
const authStore = useAuthStore()
const questions = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)
const draft = ref('')
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await listQuestions(props.productId, { page: page.value, size: PAGE_SIZE })
    questions.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  load()
}

async function handleAsk() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('請先登入')
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (!draft.value.trim()) {
    ElMessage.warning('請輸入提問內容')
    return
  }
  submitting.value = true
  try {
    await askQuestion(props.productId, draft.value)
    ElMessage.success('提問已送出,我們會盡快回覆')
    draft.value = ''
    page.value = 0
    await load()
  } finally {
    submitting.value = false
  }
}

// 切換到別的商品時重新載入(路由元件會被重用)
watch(
  () => props.productId,
  () => {
    page.value = 0
    draft.value = ''
    load()
  },
  { immediate: true },
)
</script>

<template>
  <div class="qa-section">
    <h3>商品問答({{ total }})</h3>

    <div class="ask-box">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        :placeholder="authStore.isLoggedIn ? '對商品有疑問嗎?留下你的問題' : '登入後即可提問'"
      />
      <el-button type="primary" :loading="submitting" @click="handleAsk">送出提問</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && questions.length === 0" description="還沒有人提問" :image-size="60" />
      <div v-for="q in questions" :key="q.id" class="qa-item">
        <div class="question">
          <span class="badge q">問</span>
          <div>
            <p class="text">{{ q.content }}</p>
            <p class="meta">{{ q.memberName }} · {{ q.createdAt?.slice(0, 10) }}</p>
          </div>
        </div>
        <div class="answer">
          <span class="badge a">答</span>
          <div>
            <p v-if="q.answer" class="text">{{ q.answer }}</p>
            <p v-else class="text pending">賣家尚未回覆</p>
            <p v-if="q.answeredAt" class="meta">賣家 · {{ q.answeredAt.slice(0, 10) }}</p>
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > PAGE_SIZE"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="PAGE_SIZE"
      :current-page="page + 1"
      @current-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.qa-section {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
  margin-top: 24px;
}

.ask-box {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin: 12px 0 20px;
}

.qa-item {
  padding: 12px 0;
  border-bottom: 1px dashed #eee;
}

.question,
.answer {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.answer {
  margin-top: 8px;
}

.badge {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge.q {
  background: #e4393c;
}

.badge.a {
  background: #909399;
}

.text {
  margin: 0;
  font-size: 14px;
  white-space: pre-wrap;
}

.text.pending {
  color: #999;
}

.meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: #999;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
