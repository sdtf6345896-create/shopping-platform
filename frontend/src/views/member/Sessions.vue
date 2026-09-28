<script setup>
// 登入裝置管理:查看目前登入中的裝置,登出單一裝置或其他所有裝置
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMySessions, revokeOtherSessions, revokeSession } from '../../api/session'
import { describeUserAgent, isMobileUserAgent } from '../../utils/userAgent'

const sessions = ref([])
const loading = ref(true)
const currentSessionId = localStorage.getItem('member_session_id')

const formatTime = (value) => value?.slice(0, 16).replace('T', ' ')
const others = computed(() => sessions.value.filter((s) => s.sessionId !== currentSessionId))

async function load() {
  loading.value = true
  try {
    sessions.value = await listMySessions()
  } finally {
    loading.value = false
  }
}

async function handleRevoke(session) {
  try {
    await ElMessageBox.confirm(`確定要登出「${describeUserAgent(session.userAgent)}」?`, '登出裝置', { type: 'warning' })
  } catch {
    return
  }
  await revokeSession(session.sessionId)
  ElMessage.success('已登出該裝置')
  await load()
}

async function handleRevokeOthers() {
  try {
    await ElMessageBox.confirm('其他所有裝置都會被登出,需要重新登入。確定嗎?', '登出其他裝置', { type: 'warning' })
  } catch {
    return
  }
  await revokeOtherSessions(currentSessionId)
  ElMessage.success('已登出其他裝置')
  await load()
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="header-row">
      <h3>登入裝置</h3>
      <el-button v-if="currentSessionId && others.length" type="danger" plain @click="handleRevokeOthers">
        登出其他所有裝置
      </el-button>
    </div>
    <p class="hint">
      如果看到不認得的裝置,請立即登出該裝置並修改密碼。被登出的裝置最慢幾分鐘內就需要重新登入。
    </p>

    <el-empty v-if="!loading && sessions.length === 0" description="沒有登入中的裝置" :image-size="60" />
    <div v-for="s in sessions" :key="s.sessionId" class="session" :class="{ current: s.sessionId === currentSessionId }">
      <div class="icon">{{ isMobileUserAgent(s.userAgent) ? '📱' : '💻' }}</div>
      <div class="info">
        <div class="device">
          {{ describeUserAgent(s.userAgent) }}
          <el-tag v-if="s.sessionId === currentSessionId" size="small" type="success">目前這台裝置</el-tag>
        </div>
        <div class="meta">
          IP {{ s.ipAddress || '未知' }} · 最後使用 {{ formatTime(s.lastUsedAt) || '-' }}
        </div>
      </div>
      <el-button v-if="s.sessionId !== currentSessionId" link type="danger" @click="handleRevoke(s)">登出</el-button>
    </div>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  color: #999;
  font-size: 13px;
}

.session {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.session.current {
  border-color: #b3e19d;
  background: #f6ffed;
}

.icon {
  font-size: 24px;
}

.info {
  flex: 1;
}

.device {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.meta {
  color: #999;
  font-size: 12px;
  margin-top: 2px;
}
</style>
