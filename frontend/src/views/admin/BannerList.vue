<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listAdminBanners,
  createBanner,
  updateBanner,
  updateBannerStatus,
  deleteBanner,
} from '../../api/admin/banner'
import { uploadImage } from '../../api/admin/upload'

const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const banners = ref([])
const loading = ref(true)
const uploading = ref(false)

const showDialog = ref(false)
const dialogMode = ref('create') // create | edit
const editingId = ref(null)
const formRef = ref()
const emptyForm = () => ({ title: '', subtitle: '', imageUrl: '', linkUrl: '', sortOrder: 0 })
const form = reactive(emptyForm())

const rules = {
  title: [{ required: true, message: '請輸入標題', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '請上傳圖片或輸入圖片網址', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    banners.value = await listAdminBanners()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, emptyForm())
  showDialog.value = true
}

function openEdit(banner) {
  dialogMode.value = 'edit'
  editingId.value = banner.id
  Object.assign(form, {
    title: banner.title,
    subtitle: banner.subtitle,
    imageUrl: banner.imageUrl,
    linkUrl: banner.linkUrl,
    sortOrder: banner.sortOrder,
  })
  showDialog.value = true
}

function beforeImageUpload(file) {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error('僅支援 JPG / PNG / WEBP 格式的圖片')
    return false
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error('圖片大小請控制在 5MB 以內')
    return false
  }
  return true
}

async function handleImageUpload({ file }) {
  uploading.value = true
  try {
    const data = await uploadImage(file)
    form.imageUrl = data.url
    ElMessage.success('圖片上傳成功')
  } finally {
    uploading.value = false
  }
}

async function handleSave() {
  await formRef.value.validate()
  const payload = { ...form }
  if (dialogMode.value === 'edit') {
    await updateBanner(editingId.value, payload)
  } else {
    await createBanner(payload)
  }
  ElMessage.success('儲存成功')
  showDialog.value = false
  await load()
}

async function handleToggleStatus(banner, enabled) {
  try {
    await updateBannerStatus(banner.id, enabled ? 'ACTIVE' : 'DISABLED')
    ElMessage.success('狀態更新成功')
  } finally {
    await load()
  }
}

async function handleDelete(banner) {
  try {
    await ElMessageBox.confirm(`確定要刪除「${banner.title}」嗎?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteBanner(banner.id)
  ElMessage.success('刪除成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>Banner 管理</h3>
      <el-button type="primary" @click="openCreate">新增 Banner</el-button>
    </div>

    <el-table v-loading="loading" :data="banners" class="table">
      <el-table-column label="圖片" width="140">
        <template #default="{ row }">
          <img v-if="row.imageUrl" :src="row.imageUrl" class="thumb" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="標題" min-width="140" />
      <el-table-column prop="subtitle" label="副標題" min-width="160" />
      <el-table-column prop="linkUrl" label="連結" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '啟用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'ACTIVE'"
            size="small"
            @change="(v) => handleToggleStatus(row, v)"
          />
          <el-button link size="small" @click="openEdit(row)">編輯</el-button>
          <el-button link size="small" type="danger" @click="handleDelete(row)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && banners.length === 0" description="尚無 Banner" />

    <el-dialog v-model="showDialog" :title="dialogMode === 'edit' ? '編輯 Banner' : '新增 Banner'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="標題" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="副標題">
          <el-input v-model="form.subtitle" />
        </el-form-item>
        <el-form-item label="圖片" prop="imageUrl">
          <div v-loading="uploading" class="image-upload">
            <el-upload
              class="uploader"
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleImageUpload"
              accept="image/jpeg,image/png,image/webp"
            >
              <img v-if="form.imageUrl" :src="form.imageUrl" class="preview" />
              <div v-else class="upload-placeholder">
                <el-icon :size="24"><Plus /></el-icon>
                <span>上傳圖片</span>
              </div>
            </el-upload>
            <el-input v-model="form.imageUrl" size="small" class="url-input" placeholder="或直接貼上圖片網址" />
            <p class="upload-hint">建議比例約 3:1,支援 JPG / PNG / WEBP,單檔 5MB 以內</p>
          </div>
        </el-form-item>
        <el-form-item label="連結網址">
          <el-input v-model="form.linkUrl" placeholder="例如 /products 或 https://..." />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table {
  background: #fff;
}

.thumb {
  width: 100px;
  height: 34px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.uploader :deep(.el-upload) {
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: block;
  width: 240px;
  height: 80px;
}

.uploader :deep(.el-upload):hover {
  border-color: var(--el-color-primary);
}

.preview {
  width: 240px;
  height: 80px;
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  width: 240px;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #999;
  font-size: 13px;
}

.url-input {
  width: 300px;
}

.upload-hint {
  margin: 0;
  font-size: 12px;
  color: #999;
}
</style>
