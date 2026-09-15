<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAdminProduct, createProduct, updateProduct } from '../../api/admin/product'
import { listAdminCategories } from '../../api/admin/category'
import { uploadImage } from '../../api/admin/upload'
import { flattenCategories } from '../../utils/categoryTree'

const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const props = defineProps({
  id: { type: [String, Number], default: null },
})

const router = useRouter()
const isEdit = computed(() => !!props.id)

const categories = ref([])
const flatCategories = computed(() => flattenCategories(categories.value))

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formRef = ref()

const form = reactive({
  categoryId: null,
  name: '',
  description: '',
  price: 0,
  mainImage: '',
  skus: [],
})

const rules = {
  categoryId: [{ required: true, message: '請選擇分類', trigger: 'change' }],
  name: [{ required: true, message: '請輸入商品名稱', trigger: 'blur' }],
  price: [{ required: true, message: '請輸入價格', trigger: 'blur' }],
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
    form.mainImage = data.url
    ElMessage.success('圖片上傳成功')
  } finally {
    uploading.value = false
  }
}

function addSku() {
  form.skus.push({ skuCode: '', specName: '', price: form.price || 0, stock: 0 })
}

function removeSku(index) {
  form.skus.splice(index, 1)
}

async function loadCategories() {
  categories.value = await listAdminCategories()
}

async function loadProduct() {
  loading.value = true
  try {
    const data = await getAdminProduct(props.id)
    form.categoryId = data.categoryId
    form.name = data.name
    form.description = data.description
    form.price = data.price
    form.mainImage = data.mainImage
    form.skus = data.skus.map((s) => ({
      skuCode: s.skuCode,
      specName: s.specName,
      price: s.price,
      stock: s.stock,
    }))
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (form.skus.length === 0) {
    ElMessage.warning('請至少新增一個規格(SKU)')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateProduct(props.id, form)
      ElMessage.success('更新成功')
    } else {
      await createProduct(form)
      ElMessage.success('新增成功')
    }
    router.push({ name: 'AdminProductList' })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  if (isEdit.value) {
    await loadProduct()
  } else {
    addSku()
  }
})
</script>

<template>
  <div v-loading="loading">
    <h3>{{ isEdit ? '編輯商品' : '新增商品' }}</h3>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="product-form">
      <el-form-item label="分類" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="請選擇分類" style="width: 260px">
          <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品名稱" prop="name">
        <el-input v-model="form.name" style="width: 400px" />
      </el-form-item>
      <el-form-item label="商品描述">
        <el-input v-model="form.description" type="textarea" :rows="4" style="width: 400px" />
      </el-form-item>
      <el-form-item label="價格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="商品主圖">
        <div v-loading="uploading" class="image-upload">
          <el-upload
            class="uploader"
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :http-request="handleImageUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <img v-if="form.mainImage" :src="form.mainImage" class="preview" />
            <div v-else class="upload-placeholder">
              <el-icon :size="24"><Plus /></el-icon>
              <span>上傳圖片</span>
            </div>
          </el-upload>
          <el-input v-model="form.mainImage" size="small" class="url-input" placeholder="或直接貼上圖片網址" />
          <p class="upload-hint">支援 JPG / PNG / WEBP,單檔 5MB 以內</p>
        </div>
      </el-form-item>

      <el-form-item label="規格 (SKU)">
        <div class="sku-editor">
          <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
            <el-input v-model="sku.skuCode" placeholder="SKU 編號" style="width: 160px" />
            <el-input v-model="sku.specName" placeholder="規格名稱(例:紅色/M)" style="width: 160px" />
            <el-input-number v-model="sku.price" :min="0" :precision="2" placeholder="價格" style="width: 130px" />
            <el-input-number v-model="sku.stock" :min="0" placeholder="庫存" style="width: 110px" />
            <el-button link type="danger" @click="removeSku(index)">移除</el-button>
          </div>
          <el-button size="small" @click="addSku">+ 新增規格</el-button>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSubmit">儲存</el-button>
        <el-button @click="router.push({ name: 'AdminProductList' })">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.product-form {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 24px;
  max-width: 600px;
}

.sku-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sku-row {
  display: flex;
  gap: 8px;
  align-items: center;
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
  width: 140px;
  height: 140px;
}

.uploader :deep(.el-upload):hover {
  border-color: var(--el-color-primary);
}

.preview {
  width: 140px;
  height: 140px;
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  width: 140px;
  height: 140px;
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
