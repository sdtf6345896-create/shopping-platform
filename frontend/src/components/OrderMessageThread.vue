<script setup>
// 訂單留言串:會員端與後台共用,mode 決定用哪組 API、以及哪一邊算「自己」
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderMessages, postOrderMessage } from '../api/order'
import { listAdminOrderMessages, replyOrderMessage } from '../api/admin/order'

const props = defineProps({
  orderId: { type: [String, Number], required: true },
  mode: { type: String, default: 'member', validator: (v) => ['member', 'admin'].includes(v) },
})

const api =
  props.mode === 'admin'
    ? { list: listAdminOrderMessages, post: replyOrderMessage, self: 'ADMIN' }
    : { list: getOrderMessages, post: postOrderMessage, self: 'MEMBER' }

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')

const messages = ref([])
const loading = ref(true)
const draft = ref('')
const sending = ref(false)
const listRef = ref()

function senderLabel(message) {
  if (message.sender === api.self) return '我'
  return message.sender === 'ADMIN' ? '賣家' : '買家'
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

async function load() {
  loading.value = true
  try {
    messages.value = await api.list(props.orderId)
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('請輸入留言內容')
    return
  }
  sending.value = true
  try {
    messages.value.push(await api.post(props.orderId, content))
    draft.value = ''
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="thread">
    <div v-if="messages.length" ref="listRef" class="messages">
      <div
        v-for="m in messages"
        :key="m.id"
        class="message"
        :class="{ mine: m.sender === api.self }"
      >
        <div class="meta">{{ senderLabel(m) }} · {{ formatTime(m.createdAt) }}</div>
        <div class="bubble">{{ m.content }}</div>
      </div>
    </div>
    <p v-else-if="!loading" class="empty">
      {{ mode === 'admin' ? '這張訂單還沒有留言' : '對訂單有任何問題(例如配送時間、商品規格),都可以在這裡留言給賣家' }}
    </p>
    <div class="composer">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        :placeholder="mode === 'admin' ? '回覆買家…' : '輸入留言…'"
        @keydown.ctrl.enter="send"
      />
      <el-button type="primary" :loading="sending" @click="send">{{ mode === 'admin' ? '回覆' : '送出' }}</el-button>
    </div>
  </div>
</template>

<style scoped>
.messages {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
  padding-right: 4px;
}

.message {
  max-width: 80%;
  align-self: flex-start;
}

.message.mine {
  align-self: flex-end;
  text-align: right;
}

.meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

.bubble {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f2f3f5;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

.mine .bubble {
  background: #e8f3ff;
}

.empty {
  color: #999;
  font-size: 13px;
  margin: 0 0 12px;
}

.composer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
</style>
