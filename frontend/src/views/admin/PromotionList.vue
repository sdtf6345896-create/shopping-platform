<script setup>
// 滿件折扣活動:免輸入代碼,結帳時自動套用最划算的一個,可與優惠券併用(先折活動再算優惠券)
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPromotion,
  deletePromotion,
  listAdminPromotions,
  setPromotionActive,
  updatePromotion,
} from '../../api/admin/promotion'
import { listAdminCategories } from '../../api/admin/category'
import { flattenCategories } from '../../utils/categoryTree'

const promotions = ref([])
const loading = ref(true)
const categories = ref([])
const flatCategories = computed(() => flattenCategories(categories.value))

const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formRef = ref()
const emptyForm = () => ({
  name: '',
  categoryId: null,
  minQuantity: 2,
  discountPercent: 10,
  period: null, // ['YYYY-MM-DDTHH:mm:ss', 'YYYY-MM-DDTHH:mm:ss']
  active: true,
})
const form = reactive(emptyForm())
const rules = {
  name: [{ required: true, message: '請輸入活動名稱', trigger: 'blur' }],
}

const formatTime = (value) => (value ? value.slice(0, 16).replace('T', ' ') : '')

// 例如 discountPercent 10 → 9 折、15 → 85 折
function discountLabel(percent) {
  const rate = 100 - percent
  return `${rate % 10 === 0 ? rate / 10 : rate} 折`
}

function periodLabel(p) {
  if (!p.startAt && !p.endAt) return '不限時間'
  return `${formatTime(p.startAt) || '即日起'} ~ ${formatTime(p.endAt) || '無期限'}`
}

async function load() {
  loading.value = true
  try {
    promotions.value = await listAdminPromotions()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    categoryId: row.categoryId,
    minQuantity: row.minQuantity,
    discountPercent: row.discountPercent,
    period: row.startAt || row.endAt ? [row.startAt, row.endAt] : null,
    active: row.active,
  })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  const { period, ...rest } = form
  const payload = { ...rest, startAt: period?.[0] || null, endAt: period?.[1] || null }
  saving.value = true
  try {
    if (editingId.value) {
      await updatePromotion(editingId.value, payload)
    } else {
      await createPromotion(payload)
    }
    ElMessage.success('已儲存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleToggle(row, active) {
  const updated = await setPromotionActive(row.id, active)
  Object.assign(row, updated)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`確定刪除「${row.name}」?已成立的訂單不受影響。`, '刪除活動', { type: 'warning' })
  } catch {
    return
  }
  await deletePromotion(row.id)
  ElMessage.success('已刪除')
  await load()
}

onMounted(async () => {
  await load()
  categories.value = await listAdminCategories()
})
</script>

<template>
  <div>
    <div class="header-row">
      <h3>滿件活動</h3>
      <el-button type="primary" @click="openCreate">新增活動</el-button>
    </div>
    <p class="hint">
      買家同一範圍的商品合計達指定件數,這些商品自動打折;同時符合多個活動時只套用折扣金額最大的一個,並可與優惠券併用。
    </p>

    <el-table v-loading="loading" :data="promotions" empty-text="尚未建立活動">
      <el-table-column prop="name" label="活動名稱" min-width="180" />
      <el-table-column label="範圍" width="140">
        <template #default="{ row }">{{ row.categoryName || '全站商品' }}</template>
      </el-table-column>
      <el-table-column label="條件" width="150">
        <template #default="{ row }">滿 {{ row.minQuantity }} 件 {{ discountLabel(row.discountPercent) }}</template>
      </el-table-column>
      <el-table-column label="期間" min-width="220">
        <template #default="{ row }">{{ periodLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.running" type="success" size="small">進行中</el-tag>
          <el-tag v-else-if="row.active" type="warning" size="small">未在期間</el-tag>
          <el-tag v-else type="info" size="small">已停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-switch :model-value="row.active" size="small" @change="(v) => handleToggle(row, v)" />
          <el-button link size="small" @click="openEdit(row)">編輯</el-button>
          <el-button link size="small" type="danger" @click="handleDelete(row)">刪除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '編輯活動' : '新增活動'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="活動名稱" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="例如:女裝任選 2 件 9 折" />
        </el-form-item>
        <el-form-item label="適用範圍">
          <el-select v-model="form.categoryId" placeholder="全站商品" clearable style="width: 100%">
            <el-option
              v-for="c in flatCategories"
              :key="c.id"
              :label="'　'.repeat(c.depth) + c.name"
              :value="c.id"
            />
          </el-select>
          <div class="field-hint">選擇上層分類時,底下子分類的商品也算在內</div>
        </el-form-item>
        <el-form-item label="滿幾件">
          <el-input-number v-model="form.minQuantity" :min="2" :max="99" />
        </el-form-item>
        <el-form-item label="折扣">
          <el-input-number v-model="form.discountPercent" :min="1" :max="90" />
          <span class="unit">% off({{ discountLabel(form.discountPercent) }})</span>
        </el-form-item>
        <el-form-item label="活動期間">
          <el-date-picker
            v-model="form.period"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="開始(選填)"
            end-placeholder="結束(選填)"
          />
        </el-form-item>
        <el-form-item label="啟用">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint,
.field-hint {
  color: #999;
  font-size: 12px;
}

.field-hint {
  width: 100%;
}

.unit {
  margin-left: 8px;
  color: #666;
}
</style>
