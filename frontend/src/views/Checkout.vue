<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAddresses, createAddress } from '../api/address'
import { checkout } from '../api/order'
import { applyCoupon, getMyCoupons } from '../api/coupon'
import { getPointBalance } from '../api/points'
import { maxRedeemable } from '../utils/points'
import { INVOICE_TYPE_LABELS, validateInvoice } from '../utils/invoice'
import { getShippingPolicy } from '../api/shipping'
import { CVS_BRANDS, amountToFreeShipping, shippingFeeFor, validateCvsPickup } from '../utils/shipping'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const addresses = ref([])
const selectedAddressId = ref(null)

// 配送方式:宅配用地址簿;超商取貨由會員自填門市(記住上次填的,下次免重打)
const CVS_STORAGE_KEY = 'checkout.cvsPickup'
const shippingMethod = ref('HOME_DELIVERY')
const cvsPickup = reactive({ brand: 'SEVEN_ELEVEN', storeName: '', storeCode: '', recipientName: '', recipientPhone: '' })
try {
  Object.assign(cvsPickup, JSON.parse(localStorage.getItem(CVS_STORAGE_KEY) || '{}'))
} catch {
  // 讀不到就用空白表單
}

function rememberCvsPickup() {
  try {
    localStorage.setItem(CVS_STORAGE_KEY, JSON.stringify(cvsPickup))
  } catch {
    // 無痕模式等情況存不了,不影響結帳
  }
}
const paymentMethod = ref('CREDIT_CARD')
const submitting = ref(false)
const buyerNote = ref('')
const invoice = reactive({ type: 'MEMBER_CARRIER', carrierCode: '', taxId: '', companyTitle: '', donationCode: '' })

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
const subtotalAmount = computed(() => checkoutItems.value.reduce((sum, item) => sum + item.subtotal, 0))

const couponCode = ref('')
const appliedCoupon = ref(null)
const applyingCoupon = ref(false)
const discountAmount = computed(() => appliedCoupon.value?.discountAmount || 0)

// 購物金:套用優惠券後的應付金額才是折抵上限的計算基準
const pointInfo = ref(null)
const usePoints = ref(false)
const pointsToUse = ref(0)
const payableBeforePoints = computed(() => subtotalAmount.value - discountAmount.value)
const pointLimit = computed(() =>
  pointInfo.value
    ? maxRedeemable(pointInfo.value.balance, payableBeforePoints.value, pointInfo.value.maxRedeemRatio)
    : 0,
)
const appliedPoints = computed(() => (usePoints.value ? Math.min(pointsToUse.value || 0, pointLimit.value) : 0))
const shippingPolicy = ref(null)
const shippingFee = computed(() =>
  shippingFeeFor(payableBeforePoints.value, shippingPolicy.value, shippingMethod.value),
)
const toFreeShipping = computed(() => amountToFreeShipping(payableBeforePoints.value, shippingPolicy.value))
const totalAmount = computed(() => payableBeforePoints.value - appliedPoints.value + shippingFee.value)

function handleTogglePoints(enabled) {
  pointsToUse.value = enabled ? pointLimit.value : 0
}

// 換優惠券後上限可能變小,超過就自動調回上限
watch(pointLimit, (limit) => {
  if (pointsToUse.value > limit) pointsToUse.value = limit
})

async function loadPoints() {
  try {
    pointInfo.value = await getPointBalance()
  } catch {
    pointInfo.value = null
  }
}

// 已領取的優惠券,選一張就自動帶入代碼並試算
const myCoupons = ref([])

async function handlePickCoupon(code) {
  if (!code) return
  couponCode.value = code
  await handleApplyCoupon()
}

async function handleApplyCoupon() {
  if (!couponCode.value.trim()) {
    ElMessage.warning('請輸入優惠券代碼')
    return
  }
  applyingCoupon.value = true
  try {
    appliedCoupon.value = await applyCoupon({
      code: couponCode.value.trim(),
      cartItemIds: cartStore.checkoutSelection,
    })
    ElMessage.success('優惠券套用成功')
  } catch {
    appliedCoupon.value = null
  } finally {
    applyingCoupon.value = false
  }
}

function handleRemoveCoupon() {
  appliedCoupon.value = null
  couponCode.value = ''
}

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
  const isCvs = shippingMethod.value === 'CVS_PICKUP'
  if (!isCvs && !selectedAddressId.value) {
    ElMessage.warning('請選擇或新增收件地址')
    return
  }
  const cvsError = isCvs ? validateCvsPickup(cvsPickup) : null
  if (cvsError) {
    ElMessage.warning(cvsError)
    return
  }
  if (checkoutItems.value.length === 0) {
    ElMessage.warning('沒有可結帳的商品,請重新從購物車選擇')
    router.push('/cart')
    return
  }

  const invoiceError = validateInvoice(invoice)
  if (invoiceError) {
    ElMessage.warning(invoiceError)
    return
  }

  submitting.value = true
  try {
    const order = await checkout({
      shippingMethod: shippingMethod.value,
      addressId: isCvs ? null : selectedAddressId.value,
      cvsPickup: isCvs
        ? {
            brand: cvsPickup.brand,
            storeName: cvsPickup.storeName.trim(),
            storeCode: cvsPickup.storeCode.trim() || null,
            recipientName: cvsPickup.recipientName.trim(),
            recipientPhone: cvsPickup.recipientPhone.trim(),
          }
        : null,
      paymentMethod: paymentMethod.value,
      cartItemIds: cartStore.checkoutSelection,
      couponCode: appliedCoupon.value?.code || null,
      pointsToUse: appliedPoints.value,
      note: buyerNote.value.trim() || null,
      invoice: {
        type: invoice.type,
        carrierCode: invoice.type === 'MOBILE_BARCODE' ? invoice.carrierCode.trim().toUpperCase() : null,
        taxId: invoice.type === 'COMPANY' ? invoice.taxId.trim() : null,
        companyTitle: invoice.type === 'COMPANY' ? invoice.companyTitle.trim() : null,
        donationCode: invoice.type === 'DONATION' ? invoice.donationCode.trim() : null,
      },
    })
    if (isCvs) rememberCvsPickup()
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
  await Promise.all([
    loadAddresses(),
    loadPoints(),
    getMyCoupons()
      .then((list) => {
        myCoupons.value = list
      })
      .catch(() => {}),
    getShippingPolicy()
      .then((policy) => {
        shippingPolicy.value = policy
      })
      .catch(() => {}),
  ])
})
</script>

<template>
  <div class="page-container checkout-page">
    <h2>結帳</h2>

    <section class="block">
      <div class="block-title">
        <span>配送方式</span>
      </div>
      <el-radio-group v-model="shippingMethod" class="shipping-methods">
        <el-radio-button value="HOME_DELIVERY">
          宅配到府<template v-if="shippingPolicy"> · NT$ {{ shippingPolicy.fee }}</template>
        </el-radio-button>
        <el-radio-button value="CVS_PICKUP">
          超商取貨<template v-if="shippingPolicy"> · NT$ {{ shippingPolicy.cvsFee }}</template>
        </el-radio-button>
      </el-radio-group>
      <p v-if="shippingPolicy" class="shipping-note">商品金額滿 NT$ {{ shippingPolicy.freeThreshold }} 兩種方式皆免運</p>
    </section>

    <section v-if="shippingMethod === 'CVS_PICKUP'" class="block">
      <div class="block-title">
        <span>取貨門市</span>
      </div>
      <el-form :model="cvsPickup" label-width="90px" class="cvs-form">
        <el-form-item label="超商" required>
          <el-radio-group v-model="cvsPickup.brand">
            <el-radio v-for="b in CVS_BRANDS" :key="b.value" :value="b.value">{{ b.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="門市名稱" required>
          <el-input v-model="cvsPickup.storeName" maxlength="30" placeholder="例如:信義門市" />
        </el-form-item>
        <el-form-item label="門市店號">
          <el-input v-model="cvsPickup.storeCode" maxlength="8" placeholder="選填,可於超商官網門市查詢" />
        </el-form-item>
        <el-form-item label="取件人" required>
          <el-input v-model="cvsPickup.recipientName" maxlength="50" placeholder="需與取貨時出示的證件相符" />
        </el-form-item>
        <el-form-item label="手機" required>
          <el-input v-model="cvsPickup.recipientPhone" maxlength="10" placeholder="到店通知簡訊會寄到這支手機" />
        </el-form-item>
      </el-form>
    </section>

    <section v-else class="block">
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
      <div class="block-title">優惠券</div>
      <el-select
        v-if="!appliedCoupon && myCoupons.length"
        placeholder="從我的優惠券選擇"
        class="wallet-select"
        @change="handlePickCoupon"
      >
        <el-option
          v-for="c in myCoupons"
          :key="c.id"
          :label="`${c.name}(${c.code})`"
          :value="c.code"
          :disabled="Number(c.minSpendAmount) > subtotalAmount"
        >
          <span>{{ c.name }}</span>
          <span class="wallet-hint">
            {{ Number(c.minSpendAmount) > subtotalAmount ? `滿 NT$ ${c.minSpendAmount} 可用` : c.code }}
          </span>
        </el-option>
      </el-select>
      <div v-if="!appliedCoupon" class="coupon-input">
        <el-input v-model="couponCode" placeholder="輸入優惠券代碼" @keyup.enter="handleApplyCoupon" />
        <el-button type="primary" :loading="applyingCoupon" @click="handleApplyCoupon">套用</el-button>
      </div>
      <div v-else class="coupon-applied">
        <div>
          <el-tag type="success">{{ appliedCoupon.code }}</el-tag>
          <span class="coupon-name">{{ appliedCoupon.name }}</span>
        </div>
        <el-button link type="danger" @click="handleRemoveCoupon">移除</el-button>
      </div>
    </section>

    <section v-if="pointInfo" class="block">
      <div class="block-title">
        <span>購物金</span>
        <span class="point-balance">可用 {{ pointInfo.balance }} 點</span>
      </div>
      <div class="points-row">
        <el-switch
          v-model="usePoints"
          :disabled="pointLimit === 0"
          active-text="使用購物金折抵"
          @change="handleTogglePoints"
        />
        <el-input-number
          v-if="usePoints"
          v-model="pointsToUse"
          :min="0"
          :max="pointLimit"
          :step="10"
          size="small"
        />
      </div>
      <p class="mock-hint">
        本筆最多可折抵 {{ pointLimit }} 點(應付金額的 {{ Math.round(pointInfo.maxRedeemRatio * 100) }}%),1 點 = NT$1
      </p>
    </section>

    <section class="block">
      <div class="block-title">電子發票</div>
      <el-radio-group v-model="invoice.type">
        <el-radio v-for="(label, key) in INVOICE_TYPE_LABELS" :key="key" :value="key">{{ label }}</el-radio>
      </el-radio-group>
      <div class="invoice-fields">
        <el-input
          v-if="invoice.type === 'MOBILE_BARCODE'"
          v-model="invoice.carrierCode"
          maxlength="8"
          placeholder="手機條碼,例如 /ABC1234"
        />
        <template v-else-if="invoice.type === 'COMPANY'">
          <el-input v-model="invoice.taxId" maxlength="8" placeholder="統一編號(8 碼)" />
          <el-input v-model="invoice.companyTitle" maxlength="60" placeholder="發票抬頭(公司名稱)" />
        </template>
        <el-input
          v-else-if="invoice.type === 'DONATION'"
          v-model="invoice.donationCode"
          maxlength="7"
          placeholder="愛心碼(3 到 7 位數字)"
        />
        <p v-else class="mock-hint">發票將存在您的會員帳號,中獎時會通知您。</p>
      </div>
    </section>

    <section class="block">
      <div class="block-title">訂單備註</div>
      <el-input
        v-model="buyerNote"
        type="textarea"
        :rows="2"
        maxlength="200"
        show-word-limit
        placeholder="給賣家的話,例如:請於平日白天配送(選填)"
      />
    </section>

    <section class="block">
      <div class="block-title">訂單確認</div>
      <div v-for="item in checkoutItems" :key="item.id" class="confirm-row">
        <span>{{ item.productName }} - {{ item.specName }} x {{ item.quantity }}</span>
        <span>NT$ {{ item.subtotal }}</span>
      </div>
      <div class="confirm-row">
        <span>小計</span>
        <span>NT$ {{ subtotalAmount }}</span>
      </div>
      <div v-if="appliedCoupon" class="confirm-row discount-row">
        <span>優惠折抵</span>
        <span>- NT$ {{ discountAmount }}</span>
      </div>
      <div v-if="appliedPoints > 0" class="confirm-row discount-row">
        <span>購物金折抵</span>
        <span>- NT$ {{ appliedPoints }}</span>
      </div>
      <div v-if="shippingPolicy" class="confirm-row">
        <span>
          運費
          <small v-if="toFreeShipping > 0" class="shipping-hint">再買 NT$ {{ toFreeShipping }} 免運</small>
        </span>
        <span>{{ shippingFee > 0 ? `NT$ ${shippingFee}` : '免運' }}</span>
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
.shipping-methods {
  margin-bottom: 4px;
}

.shipping-note {
  margin: 6px 0 0;
  color: #999;
  font-size: 12px;
}

.cvs-form {
  max-width: 480px;
}

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

.invoice-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  max-width: 360px;
}

.wallet-select {
  width: 100%;
  margin-bottom: 8px;
}

.wallet-hint {
  float: right;
  color: #999;
  font-size: 12px;
}

.shipping-hint {
  margin-left: 6px;
  color: #e4393c;
}

.point-balance {
  font-weight: normal;
  font-size: 13px;
  color: #e4393c;
}

.points-row {
  display: flex;
  align-items: center;
  gap: 16px;
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

.discount-row {
  color: #e4393c;
}

.coupon-input {
  display: flex;
  gap: 12px;
}

.coupon-applied {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.coupon-name {
  margin-left: 8px;
  font-size: 13px;
  color: #666;
}

.total-amount {
  color: #e4393c;
  font-size: 18px;
}

.submit-btn {
  width: 100%;
}
</style>
