<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAddresses, createAddress } from '../api/address'
import { checkout } from '../api/order'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const addresses = ref([])
const selectedAddressId = ref(null)
const paymentMethod = ref('CREDIT_CARD')
const submitting = ref(false)

const showAddressDialog = ref(false)
const addressFormRef = ref()
const addressForm = reactive({
  recipientName: '',
  phone: '',
  postalCode: '',
  city: '',
  district: '',
  detailAddress: '',
  defaultAddress: false,
})

const addressRules = {
  recipientName: [{ required: true, message: '請輸入收件人姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '請輸入電話', trigger: 'blur' }],
  city: [{ required: true, message: '請輸入縣市', trigger: 'blur' }],
  district: [{ required: true, message: '請輸入鄉鎮區', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '請輸入詳細地址', trigger: 'blur' }],
}

const checkoutItems = computed(() => {
  const ids = cartStore.checkoutSelection || []
  return cartStore.items.filter((item) => ids.includes(item.id))
})
const totalAmount = computed(() => checkoutItems.value.reduce((sum, item) => sum + item.subtotal, 0))

async function loadAddresses() {
  addresses.value = await listAddresses()
  const defaultAddr = addresses.value.find((a) => a.defaultAddress)
  selectedAddressId.value = (defaultAddr || addresses.value[0])?.id ?? null
}

async function handleCreateAddress() {
  await addressFormRef.value.validate()
  const created = await createAddress(addressForm)
  ElMessage.success('地址新增成功')
  showAddressDialog.value = false
  await loadAddresses()
  selectedAddressId.value = created.id
  Object.assign(addressForm, {
    recipientName: '',
    phone: '',
    postalCode: '',
    city: '',
    district: '',
    detailAddress: '',
    defaultAddress: false,
  })
}

async function handleSubmit() {
  if (!selectedAddressId.value) {
    ElMessage.warning('請選擇或新增收件地址')
    return
  }
  if (checkoutItems.value.length === 0) {
    ElMessage.warning('沒有可結帳的商品,請重新從購物車選擇')
    router.push('/cart')
    return
  }

  submitting.value = true
  try {
    const order = await checkout({
      addressId: selectedAddressId.value,
      paymentMethod: paymentMethod.value,
      cartItemIds: cartStore.checkoutSelection,
    })
    cartStore.setCheckoutSelection(null)
    await cartStore.fetchCart()
    ElMessage.success('訂單建立成功')
    router.push(`/orders/${order.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!cartStore.checkoutSelection) {
    router.replace('/cart')
    return
  }
  await loadAddresses()
})
</script>

<template>
  <div class="page-container checkout-page">
    <h2>結帳</h2>

    <section class="block">
      <div class="block-title">
        <span>收件資訊</span>
        <el-button size="small" @click="showAddressDialog = true">新增地址</el-button>
      </div>
      <el-empty v-if="addresses.length === 0" description="尚無收件地址,請先新增" :image-size="60" />
      <el-radio-group v-else v-model="selectedAddressId" class="address-list">
        <el-radio v-for="addr in addresses" :key="addr.id" :value="addr.id" class="address-item">
          <div>
            <strong>{{ addr.recipientName }}</strong> {{ addr.phone }}
            <el-tag v-if="addr.defaultAddress" size="small" type="success">預設</el-tag>
            <p class="addr-text">{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</p>
          </div>
        </el-radio>
      </el-radio-group>
    </section>

    <section class="block">
      <div class="block-title">付款方式</div>
      <el-radio-group v-model="paymentMethod">
        <el-radio value="CREDIT_CARD">信用卡</el-radio>
        <el-radio value="ATM">ATM 轉帳</el-radio>
        <el-radio value="COD">貨到付款</el-radio>
      </el-radio-group>
      <p class="mock-hint">※ 本專案付款流程為模擬,不會實際扣款</p>
    </section>

    <section class="block">
      <div class="block-title">訂單確認</div>
      <div v-for="item in checkoutItems" :key="item.id" class="confirm-row">
        <span>{{ item.productName }} - {{ item.specName }} x {{ item.quantity }}</span>
        <span>NT$ {{ item.subtotal }}</span>
      </div>
      <div class="confirm-total">
        <span>總金額</span>
        <span class="total-amount">NT$ {{ totalAmount }}</span>
      </div>
    </section>

    <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
      送出訂單
    </el-button>

    <el-dialog v-model="showAddressDialog" title="新增收件地址" width="420px">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="90px">
        <el-form-item label="收件人" prop="recipientName">
          <el-input v-model="addressForm.recipientName" />
        </el-form-item>
        <el-form-item label="電話" prop="phone">
          <el-input v-model="addressForm.phone" />
        </el-form-item>
        <el-form-item label="郵遞區號">
          <el-input v-model="addressForm.postalCode" />
        </el-form-item>
        <el-form-item label="縣市" prop="city">
          <el-input v-model="addressForm.city" />
        </el-form-item>
        <el-form-item label="鄉鎮區" prop="district">
          <el-input v-model="addressForm.district" />
        </el-form-item>
        <el-form-item label="詳細地址" prop="detailAddress">
          <el-input v-model="addressForm.detailAddress" />
        </el-form-item>
        <el-form-item label="設為預設">
          <el-switch v-model="addressForm.defaultAddress" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAddress">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 720px;
}

.block {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-item {
  align-items: flex-start;
  height: auto;
  white-space: normal;
}

.addr-text {
  margin: 4px 0 0;
  color: #666;
  font-size: 13px;
}

.mock-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px dashed #eee;
}

.confirm-total {
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
  font-weight: 600;
}

.total-amount {
  color: #e4393c;
  font-size: 18px;
}

.submit-btn {
  width: 100%;
}
</style>
