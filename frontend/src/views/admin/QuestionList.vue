<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { answerQuestion, deleteQuestion, listAdminQuestions } from '../../api/admin/question'

const router = useRouter()
const questions = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  tab: 'pending', // pending | answered | all
  page: 0,
})

const ANSWERED_PARAM = { pending: false, answered: true, all: undefined }

async function load() {
  loading.value = true
  try {
    const data = await listAdminQuestions({
      answered: ANSWERED_PARAM[filters.tab],
      page: filters.page,
      size: 10,
    })
    questions.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  filters.page = 0
  load()
}

function handlePageChange(page) {
  filters.page = page - 1
  load()
}

const dialog = reactive({ visible: false, question: null, answer: '', saving: false })

function openAnswer(question) {
  dialog.question = question
  dialog.answer = question.answer || ''
  dialog.visible = true
}

async function submitAnswer() {
  if (!dialog.answer.trim()) {
    ElMessage.warning('請輸入回覆內容')
    return
  }
  dialog.saving = true
  try {
    const firstAnswer = !dialog.question.answer
    await answerQuestion(dialog.question.id, dialog.answer)
    ElMessage.success(firstAnswer ? '回覆成功,已通知提問會員' : '回覆已更新')
    dialog.visible = false
    await load()
  } finally {
    dialog.saving = false
  }
}

async function handleDelete(question) {
  try {
    await ElMessageBox.confirm('確定要刪除這則提問嗎?', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteQuestion(question.id)
  ElMessage.success('刪除成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>商品問答</h3>
    </div>

    <el-tabs v-model="filters.tab" @tab-change="handleTabChange">
      <el-tab-pane label="待回覆" name="pending" />
      <el-tab-pane label="已回覆" name="answered" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <el-table v-loading="loading" :data="questions">
      <el-table-column label="商品" min-width="160">
        <template #default="{ row }">
          <el-link
            type="primary"
            :underline="false"
            @click="router.push({ name: 'AdminProductEdit', params: { id: row.productId } })"
          >
            {{ row.productName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="提問" min-width="240">
        <template #default="{ row }">
          <div class="question-text">{{ row.content }}</div>
          <div v-if="row.answer" class="answer-text">答:{{ row.answer }}</div>
        </template>
      </el-table-column>
      <el-table-column label="會員" width="180">
        <template #default="{ row }">
          <div>{{ row.memberName }}</div>
          <div class="sub">{{ row.memberEmail }}</div>
        </template>
      </el-table-column>
      <el-table-column label="提問時間" width="150">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 16).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="openAnswer(row)">
            {{ row.answer ? '編輯回覆' : '回覆' }}
          </el-button>
          <el-button link size="small" type="danger" @click="handleDelete(row)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="10"
      :current-page="filters.page + 1"
      @current-change="handlePageChange"
    />

    <el-dialog v-model="dialog.visible" title="回覆提問" width="520px">
      <template v-if="dialog.question">
        <p class="dialog-product">{{ dialog.question.productName }}</p>
        <p class="dialog-question">問:{{ dialog.question.content }}</p>
        <el-input v-model="dialog.answer" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        <p v-if="!dialog.question.answer" class="hint">送出後會寄 email 通知提問的會員。</p>
      </template>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="submitAnswer">送出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question-text {
  white-space: pre-wrap;
}

.answer-text {
  color: #67c23a;
  font-size: 13px;
  margin-top: 4px;
  white-space: pre-wrap;
}

.sub {
  color: #999;
  font-size: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.dialog-product {
  font-weight: 600;
  margin: 0 0 4px;
}

.dialog-question {
  color: #666;
  margin: 0 0 12px;
  white-space: pre-wrap;
}

.hint {
  color: #999;
  font-size: 12px;
  margin: 8px 0 0;
}
</style>
