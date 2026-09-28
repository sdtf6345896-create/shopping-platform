<script setup>
// 後台內部備註(訂單 / 會員共用),會員看不到
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addAdminNote, deleteAdminNote, listAdminNotes } from '../api/admin/note'
import { useAdminAuthStore } from '../stores/adminAuth'

const props = defineProps({
  targetType: { type: String, required: true, validator: (v) => ['ORDER', 'MEMBER'].includes(v) },
  targetId: { type: [String, Number], required: true },
})

const adminAuthStore = useAdminAuthStore()
const notes = ref([])
const draft = ref('')
const saving = ref(false)
const loading = ref(true)

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

// 重新整理後 store 可能還沒有管理員資料,這時一律顯示刪除鈕,由後端擋下刪別人的備註
const canDelete = (note) => !adminAuthStore.admin || adminAuthStore.admin.username === note.adminUsername

async function load() {
  loading.value = true
  try {
    notes.value = await listAdminNotes(props.targetType, props.targetId)
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('請輸入備註內容')
    return
  }
  saving.value = true
  try {
    notes.value.unshift(await addAdminNote({ targetType: props.targetType, targetId: props.targetId, content }))
    draft.value = ''
  } finally {
    saving.value = false
  }
}

async function handleDelete(note) {
  try {
    await ElMessageBox.confirm('確定刪除這則備註?', '刪除備註', { type: 'warning' })
  } catch {
    return
  }
  await deleteAdminNote(note.id)
  notes.value = notes.value.filter((n) => n.id !== note.id)
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="admin-notes">
    <div class="composer">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        placeholder="內部備註,只有管理員看得到(例如:客人來電要求改週六配送)"
      />
      <el-button type="primary" :loading="saving" @click="handleAdd">新增</el-button>
    </div>
    <p v-if="!loading && notes.length === 0" class="empty">尚無備註</p>
    <div v-for="note in notes" :key="note.id" class="note">
      <div class="meta">
        <span>{{ note.adminUsername }} · {{ formatTime(note.createdAt) }}</span>
        <el-button v-if="canDelete(note)" link size="small" type="danger" @click="handleDelete(note)">刪除</el-button>
      </div>
      <div class="content">{{ note.content }}</div>
    </div>
  </div>
</template>

<style scoped>
.composer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-bottom: 12px;
}

.empty {
  color: #999;
  font-size: 13px;
  margin: 0;
}

.note {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #fffbe6;
  border-left: 3px solid #e6a23c;
  border-radius: 4px;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 12px;
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}
</style>
