<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAddresses, createAddress, updateAddress, deleteAddress } from '../../api/address'

const addresses = ref([])
const loading = ref(true)
const showDialog = ref(false)
const editingId = ref(null)
const formRef = ref()

const emptyForm = () => ({
  recipientName: '',
  phone: '',
  postalCode: '',
  city: '',
  district: '',
  detailAddress: '',
  defaultAddress: false,
})

const form = reactive(emptyForm())

const rules = {
  recipientName: [{ required: true, message: '請輸入收件人姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '請輸入電話', trigger: 'blur' }],
  city: [{ required: true, message: '請輸入縣市', trigger: 'blur' }],
  district: [{ required: true, message: '請輸入鄉鎮區', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '請輸入詳細地址', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    addresses.value = await listAddresses()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  showDialog.value = true
}

function openEdit(addr) {
  editingId.value = addr.id
  Object.assign(form, {
    recipientName: addr.recipientName,
    phone: addr.phone,
    postalCode: addr.postalCode,
    city: addr.city,
    district: addr.district,
    detailAddress: addr.detailAddress,
    defaultAddress: addr.defaultAddress,
  })
  showDialog.value = true
}

async function handleSave() {
  await formRef.value.validate()
  if (editingId.value) {
    await updateAddress(editingId.value, form)
  } else {
    await createAddress(form)
  }
  ElMessage.success('儲存成功')
  showDialog.value = false
  await load()
}

async function handleDelete(addr) {
  await ElMessageBox.confirm(`確定要刪除「${addr.recipientName}」的地址嗎?`, '提示', { type: 'warning' })
  await deleteAddress(addr.id)
  ElMessage.success('刪除成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="header-row">
      <h3>收件地址</h3>
      <el-button type="primary" size="small" @click="openCreate">新增地址</el-button>
    </div>

    <el-empty v-if="!loading && addresses.length === 0" description="尚無收件地址" />

    <div v-for="addr in addresses" :key="addr.id" class="address-card">
      <div>
        <strong>{{ addr.recipientName }}</strong> {{ addr.phone }}
        <el-tag v-if="addr.defaultAddress" size="small" type="success">預設</el-tag>
        <p class="addr-text">{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</p>
      </div>
      <div class="actions">
        <el-button link @click="openEdit(addr)">編輯</el-button>
        <el-button link type="danger" @click="handleDelete(addr)">刪除</el-button>
      </div>
    </div>

    <el-dialog v-model="showDialog" :title="editingId ? '編輯地址' : '新增地址'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收件人" prop="recipientName">
          <el-input v-model="form.recipientName" />
        </el-form-item>
        <el-form-item label="電話" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="郵遞區號">
          <el-input v-model="form.postalCode" />
        </el-form-item>
        <el-form-item label="縣市" prop="city">
          <el-input v-model="form.city" />
        </el-form-item>
        <el-form-item label="鄉鎮區" prop="district">
          <el-input v-model="form.district" />
        </el-form-item>
        <el-form-item label="詳細地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" />
        </el-form-item>
        <el-form-item label="設為預設">
          <el-switch v-model="form.defaultAddress" />
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

.address-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.addr-text {
  margin: 6px 0 0;
  color: #666;
  font-size: 13px;
}
</style>
