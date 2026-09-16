<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAdminCoupons,
  createCoupon,
  updateCoupon,
  updateCouponStatus,
  deleteCoupon,
} from '../../api/admin/coupon'
import { COUPON_STATUS_LABELS, DISCOUNT_TYPE_LABELS, formatDiscount } from '../../utils/couponEnums'

const coupons = ref([])
const total = ref(0)
const loading = ref(true)

const filters = reactive({
  keyword: '',
  status: null,
  page: 0,
})

const showDialog = ref(false)
const dialogMode = ref('create') // create | edit
const editingId = ref(null)
const formRef = ref()
const emptyForm = () => ({
  code: '',
  name: '',
  discountType: 'FIXED_AMOUNT',
  discountValue: 0,
  maxDiscountAmount: null,
  minSpendAmount: 0,
  totalQuantity: null,
  startAt: null,
  endAt: null,
})
const form = reactive(emptyForm())

const rules = {
  code: [{ required: true, message: '請輸入優惠券代碼', trigger: 'blur' }],
  name: [{ required: true, message: '請輸入優惠券名稱', trigger: 'blur' }],
  discountType: [{ required: true, message: '請選擇折扣類型', trigger: 'change' }],
  discountValue: [{ required: true, message: '請輸入折扣數值', trigger: 'blur' }],
  minSpendAmount: [{ required: true, message: '請輸入最低消費金額', trigger: 'blur' }],
}

async function loadCoupons() {
  loading.value = true
  try {
    const data = await listAdminCoupons({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page: filters.page,
      size: 10,
    })
    coupons.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 0
  loadCoupons()
}

function handlePageChange(page) {
  filters.page = page - 1
  loadCoupons()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, emptyForm())
  showDialog.value = true
}

function openEdit(coupon) {
  dialogMode.value = 'edit'
  editingId.value = coupon.id
  Object.assign(form, {
    code: coupon.code,
    name: coupon.name,
    discountType: coupon.discountType,
    discountValue: coupon.discountValue,
    maxDiscountAmount: coupon.maxDiscountAmount,
    minSpendAmount: coupon.minSpendAmount,
    totalQuantity: coupon.totalQuantity,
    startAt: coupon.startAt,
    endAt: coupon.endAt,
  })
  showDialog.value = true
}

async function handleSave() {
  await formRef.value.validate()
  const payload = { ...form }
  if (payload.discountType === 'FIXED_AMOUNT') {
    payload.maxDiscountAmount = null
  }
  if (dialogMode.value === 'edit') {
    await updateCoupon(editingId.value, payload)
  } else {
    await createCoupon(payload)
  }
  ElMessage.success('儲存成功')
  showDialog.value = false
  await loadCoupons()
}

async function handleToggleStatus(coupon, enabled) {
  try {
    await updateCouponStatus(coupon.id, enabled ? 'ACTIVE' : 'DISABLED')
    ElMessage.success('狀態更新成功')
  } finally {
    await loadCoupons()
  }
}

async function handleDelete(coupon) {
  try {
    await ElMessageBox.confirm(`確定要刪除優惠券「${coupon.code}」嗎?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteCoupon(coupon.id)
    ElMessage.success('刪除成功')
    await loadCoupons()
  } catch {
    // 錯誤訊息已由全域攔截器顯示(例如優惠券已被使用)
  }
}

onMounted(loadCoupons)
</script>

<template>
  <div>
    <div class="header-row">
      <h3>優惠券管理</h3>
      <el-button type="primary" @click="openCreate">新增優惠券</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="全部狀態" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="(label, value) in COUPON_STATUS_LABELS" :key="value" :label="label" :value="value" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜尋代碼或名稱"
        style="width: 220px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button @click="handleSearch">搜尋</el-button>
    </div>

    <el-table v-loading="loading" :data="coupons" class="table">
      <el-table-column prop="code" label="代碼" width="140" />
      <el-table-column prop="name" label="名稱" min-width="160" />
      <el-table-column label="折扣內容" min-width="180">
        <template #default="{ row }">{{ formatDiscount(row) }}</template>
      </el-table-column>
      <el-table-column label="最低消費" width="110">
        <template #default="{ row }">NT$ {{ row.minSpendAmount }}</template>
      </el-table-column>
      <el-table-column label="使用狀況" width="120">
        <template #default="{ row }">{{ row.usedQuantity }} / {{ row.totalQuantity ?? '無限制' }}</template>
      </el-table-column>
      <el-table-column label="有效期間" min-width="200">
        <template #default="{ row }">
          <span v-if="!row.startAt && !row.endAt">不限期</span>
          <span v-else>
            {{ row.startAt ? row.startAt.slice(0, 16).replace('T', ' ') : '不限' }}
            ~
            {{ row.endAt ? row.endAt.slice(0, 16).replace('T', ' ') : '不限' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ COUPON_STATUS_LABELS[row.status] }}
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

    <el-dialog v-model="showDialog" :title="dialogMode === 'edit' ? '編輯優惠券' : '新增優惠券'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="代碼" prop="code">
          <el-input v-model="form.code" :disabled="dialogMode === 'edit'" placeholder="例如 SAVE100" />
        </el-form-item>
        <el-form-item label="名稱" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="折扣類型" prop="discountType">
          <el-radio-group v-model="form.discountType">
            <el-radio v-for="(label, value) in DISCOUNT_TYPE_LABELS" :key="value" :value="value">
              {{ label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="form.discountType === 'PERCENTAGE' ? '折扣百分比' : '折抵金額'"
          prop="discountValue"
        >
          <el-input-number v-model="form.discountValue" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="form.discountType === 'PERCENTAGE'" label="折抵上限">
          <el-input-number v-model="form.maxDiscountAmount" :min="0" :precision="2" placeholder="不限制" />
        </el-form-item>
        <el-form-item label="最低消費金額" prop="minSpendAmount">
          <el-input-number v-model="form.minSpendAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="發放張數上限">
          <el-input-number v-model="form.totalQuantity" :min="1" placeholder="不限制" />
        </el-form-item>
        <el-form-item label="開始時間">
          <el-date-picker v-model="form.startAt" type="datetime" placeholder="不限制" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="結束時間">
          <el-date-picker v-model="form.endAt" type="datetime" placeholder="不限制" value-format="YYYY-MM-DDTHH:mm:ss" />
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.table {
  background: #fff;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
