<script setup>
import StockMovementDialog from '../../components/StockMovementDialog.vue'
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

// 已存檔的規格(有 id 才查得到庫存異動紀錄);以 SKU 編號對應表單列
const savedSkus = ref([])
const historySku = ref(null)
const savedSkuOf = (row) => savedSkus.value.find((s) => s.skuCode === row.skuCode)

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
  images: [],
  skus: [],
  // 規格表:[{ name: '材質', value: '純棉' }]
  specs: [],
  // 限時特價
  saleEnabled: false,
  saleDiscountPercent: 20,
  salePeriod: null, // ['YYYY-MM-DDTHH:mm:ss', 'YYYY-MM-DDTHH:mm:ss']
  // 排程上架 / 下架(選填,時間到由後端排程自動切換狀態)
  publishAt: null,
  unpublishAt: null,
})

// 排程只能選未來的時間
const disablePastDate = (date) => date.getTime() < new Date().setHours(0, 0, 0, 0)

// 送出時把表單轉成 API 需要的格式(特價關閉時三個欄位都送 null)
function buildPayload() {
  const { saleEnabled, saleDiscountPercent, salePeriod, specs, ...rest } = form
  return {
    ...rest,
    // 整列空白的規格列直接略過
    specs: specs
      .map((s) => ({ name: s.name.trim(), value: s.value.trim() }))
      .filter((s) => s.name || s.value),
    saleDiscountPercent: saleEnabled ? saleDiscountPercent : null,
    saleStartAt: saleEnabled ? salePeriod?.[0] : null,
    saleEndAt: saleEnabled ? salePeriod?.[1] : null,
  }
}

const MAX_GALLERY_IMAGES = 8
const uploadingGallery = ref(false)

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

async function handleGalleryUpload({ file }) {
  if (form.images.length >= MAX_GALLERY_IMAGES) {
    ElMessage.warning(`商品圖片最多 ${MAX_GALLERY_IMAGES} 張`)
    return
  }
  uploadingGallery.value = true
  try {
    const data = await uploadImage(file)
    form.images.push(data.url)
  } finally {
    uploadingGallery.value = false
  }
}

function removeGalleryImage(index) {
  form.images.splice(index, 1)
}

function moveGalleryImage(index, offset) {
  const target = index + offset
  if (target < 0 || target >= form.images.length) return
  const [image] = form.images.splice(index, 1)
  form.images.splice(target, 0, image)
}

function addSku() {
  form.skus.push({ skuCode: '', specName: '', price: form.price || 0, stock: 0 })
}

function removeSku(index) {
  form.skus.splice(index, 1)
}

const MAX_SPECS = 20

function addSpec() {
  form.specs.push({ name: '', value: '' })
}

function removeSpec(index) {
  form.specs.splice(index, 1)
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
    form.images = [...(data.images || [])]
    form.specs = (data.specs || []).map((s) => ({ ...s }))
    form.saleEnabled = data.saleDiscountPercent != null
    form.saleDiscountPercent = data.saleDiscountPercent ?? 20
    form.salePeriod = data.saleStartAt && data.saleEndAt ? [data.saleStartAt, data.saleEndAt] : null
    form.publishAt = data.publishAt
    form.unpublishAt = data.unpublishAt
    form.skus = data.skus.map((s) => ({
      skuCode: s.skuCode,
      specName: s.specName,
      price: s.price,
      stock: s.stock,
    }))
    savedSkus.value = data.skus
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (form.saleEnabled && !form.salePeriod) {
    ElMessage.warning('請選擇限時特價的開始與結束時間')
    return
  }
  if (form.skus.length === 0) {
    ElMessage.warning('請至少新增一個規格(SKU)')
    return
  }
  if (form.specs.some((s) => !s.name.trim() !== !s.value.trim())) {
    ElMessage.warning('規格表的每一列都需要填寫項目與內容')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateProduct(props.id, buildPayload())
      ElMessage.success('更新成功')
    } else {
      await createProduct(buildPayload())
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
      <el-form-item label="規格表">
        <div class="spec-editor">
          <div v-for="(spec, index) in form.specs" :key="index" class="spec-row">
            <el-input v-model="spec.name" placeholder="項目(例:材質)" maxlength="30" style="width: 140px" />
            <el-input v-model="spec.value" placeholder="內容(例:100% 純棉)" maxlength="200" style="width: 260px" />
            <el-button link type="danger" @click="removeSpec(index)">移除</el-button>
          </div>
          <el-button v-if="form.specs.length < MAX_SPECS" size="small" @click="addSpec">+ 新增規格列</el-button>
        </div>
      </el-form-item>
      <el-form-item label="價格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="限時特價">
        <div class="sale-editor">
          <el-switch v-model="form.saleEnabled" active-text="啟用" />
          <template v-if="form.saleEnabled">
            <span>折扣</span>
            <el-input-number v-model="form.saleDiscountPercent" :min="1" :max="90" size="small" />
            <span>%</span>
            <el-date-picker
              v-model="form.salePeriod"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="開始時間"
              end-placeholder="結束時間"
              size="small"
            />
          </template>
        </div>
      </el-form-item>

      <el-form-item label="排程上下架">
        <div class="sale-editor">
          <el-date-picker
            v-model="form.publishAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="自動上架時間(選填)"
            :disabled-date="disablePastDate"
            size="small"
          />
          <el-date-picker
            v-model="form.unpublishAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="自動下架時間(選填)"
            :disabled-date="disablePastDate"
            size="small"
          />
        </div>
        <div class="field-hint">時間到會自動切換上架狀態(約 1 分鐘內生效);新品預告可先存成「下架」並設定上架時間</div>
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

      <el-form-item label="商品圖庫">
        <div v-loading="uploadingGallery" class="gallery-editor">
          <div v-for="(url, index) in form.images" :key="url + index" class="gallery-item">
            <img :src="url" class="gallery-thumb" />
            <div class="gallery-actions">
              <el-button link size="small" :disabled="index === 0" @click="moveGalleryImage(index, -1)">←</el-button>
              <el-button link size="small" type="danger" @click="removeGalleryImage(index)">移除</el-button>
              <el-button
                link
                size="small"
                :disabled="index === form.images.length - 1"
                @click="moveGalleryImage(index, 1)"
              >
                →
              </el-button>
            </div>
          </div>
          <el-upload
            v-if="form.images.length < MAX_GALLERY_IMAGES"
            class="gallery-uploader"
            multiple
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :http-request="handleGalleryUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <div class="gallery-placeholder">
              <el-icon :size="20"><Plus /></el-icon>
              <span>新增圖片</span>
            </div>
          </el-upload>
        </div>
        <p class="upload-hint">主圖以外的商品圖片,最多 {{ MAX_GALLERY_IMAGES }} 張,可調整順序</p>
      </el-form-item>

      <el-form-item label="規格 (SKU)">
        <div class="sku-editor">
          <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
            <el-input v-model="sku.skuCode" placeholder="SKU 編號" style="width: 160px" />
            <el-input v-model="sku.specName" placeholder="規格名稱(例:紅色/M)" style="width: 160px" />
            <el-input-number v-model="sku.price" :min="0" :precision="2" placeholder="價格" style="width: 130px" />
            <el-input-number v-model="sku.stock" :min="0" placeholder="庫存" style="width: 110px" />
            <el-button v-if="savedSkuOf(sku)" link @click="historySku = savedSkuOf(sku)">異動紀錄</el-button>
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

    <StockMovementDialog :product-id="props.id" :sku="historySku" @close="historySku = null" />
  </div>
</template>

<style scoped>
.spec-editor {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.spec-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.field-hint {
  width: 100%;
  color: #999;
  font-size: 12px;
}

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

.sale-editor {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.gallery-editor {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.gallery-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.gallery-thumb {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #eee;
}

.gallery-actions {
  display: flex;
  gap: 2px;
}

.gallery-uploader :deep(.el-upload) {
  width: 96px;
  height: 96px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
}

.gallery-uploader :deep(.el-upload):hover {
  border-color: var(--el-color-primary);
}

.gallery-placeholder {
  width: 96px;
  height: 96px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #999;
  font-size: 12px;
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
