<script setup>
import { computed } from 'vue'
import { PAYMENT_METHOD_LABELS } from '../utils/orderEnums'

const props = defineProps({
  order: { type: Object, required: true },
  // receipt:給會員的收據(含金額);packing:倉庫揀貨單(不含金額,含 SKU 與勾選欄)
  variant: { type: String, default: 'receipt', validator: (v) => ['receipt', 'packing'].includes(v) },
})

const isReceipt = computed(() => props.variant === 'receipt')
const totalQuantity = computed(() => props.order.items.reduce((sum, item) => sum + item.quantity, 0))
const printedAt = new Date().toLocaleString('zh-TW', { hour12: false })
const formatTime = (value) => value?.slice(0, 19).replace('T', ' ')
</script>

<template>
  <article class="printable" :class="variant">
    <header class="doc-header">
      <div>
        <h1 class="shop">MomoShop</h1>
        <p class="doc-title">{{ isReceipt ? '購物收據' : '揀貨單' }}</p>
      </div>
      <div class="doc-meta">
        <p>訂單編號:<strong>{{ order.orderNo }}</strong></p>
        <p>下單時間:{{ formatTime(order.createdAt) }}</p>
        <p v-if="isReceipt">付款方式:{{ PAYMENT_METHOD_LABELS[order.paymentMethod] }}</p>
        <p>列印時間:{{ printedAt }}</p>
      </div>
    </header>

    <section class="receiver">
      <p><span class="label">收件人</span>{{ order.receiverName }} {{ order.receiverPhone }}</p>
      <p><span class="label">地址</span>{{ order.receiverAddress }}</p>
      <p v-if="order.buyerNote"><span class="label">備註</span>{{ order.buyerNote }}</p>
      <p v-if="order.trackingNumber">
        <span class="label">物流</span>{{ order.shippingCarrier }} {{ order.trackingNumber }}
      </p>
    </section>

    <table class="items">
      <thead>
        <tr>
          <th v-if="!isReceipt" class="check">✓</th>
          <th v-if="!isReceipt">SKU</th>
          <th>商品</th>
          <th>規格</th>
          <th class="num">數量</th>
          <th v-if="isReceipt" class="num">單價</th>
          <th v-if="isReceipt" class="num">小計</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in order.items" :key="item.id">
          <td v-if="!isReceipt" class="check">☐</td>
          <td v-if="!isReceipt" class="sku">{{ item.skuCode }}</td>
          <td>{{ item.productName }}</td>
          <td>{{ item.specName }}</td>
          <td class="num">{{ item.quantity }}</td>
          <td v-if="isReceipt" class="num">{{ item.unitPrice }}</td>
          <td v-if="isReceipt" class="num">{{ item.subtotal }}</td>
        </tr>
      </tbody>
    </table>

    <section v-if="isReceipt" class="totals">
      <p><span>商品小計</span><span>NT$ {{ order.subtotalAmount }}</span></p>
      <p v-if="order.discountAmount > 0">
        <span>優惠折抵{{ order.couponCode ? `(${order.couponCode})` : '' }}</span>
        <span>- NT$ {{ order.discountAmount }}</span>
      </p>
      <p v-if="order.pointsUsed > 0"><span>購物金折抵</span><span>- NT$ {{ order.pointsUsed }}</span></p>
      <p><span>運費</span><span>{{ order.shippingFee > 0 ? `NT$ ${order.shippingFee}` : '免運' }}</span></p>
      <p class="grand"><span>實付金額</span><span>NT$ {{ order.totalAmount }}</span></p>
      <p class="note">本收據僅供購物證明,非統一發票。</p>
    </section>
    <section v-else class="totals">
      <p class="grand"><span>商品總件數</span><span>{{ totalQuantity }} 件</span></p>
      <p class="sign">揀貨人員:________________  覆核:________________</p>
    </section>
  </article>
</template>

<style scoped>
.printable {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px;
  background: #fff;
  color: #222;
  font-size: 14px;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  border-bottom: 2px solid #222;
  padding-bottom: 12px;
}

.shop {
  margin: 0;
  font-size: 24px;
  color: #e4393c;
}

.doc-title {
  margin: 4px 0 0;
  font-size: 18px;
  font-weight: 600;
}

.doc-meta p,
.receiver p,
.totals p {
  margin: 3px 0;
}

.receiver {
  margin: 16px 0;
}

.label {
  display: inline-block;
  width: 56px;
  color: #666;
}

.items {
  width: 100%;
  border-collapse: collapse;
}

.items th,
.items td {
  border-bottom: 1px solid #ddd;
  padding: 8px 6px;
  text-align: left;
}

.items th {
  background: #f5f5f5;
}

.num {
  text-align: right !important;
}

.check {
  width: 32px;
  text-align: center !important;
  font-size: 16px;
}

.sku {
  font-family: monospace;
}

.totals {
  margin-left: auto;
  margin-top: 16px;
  width: 320px;
}

.totals p {
  display: flex;
  justify-content: space-between;
}

.grand {
  border-top: 1px solid #222;
  padding-top: 6px;
  font-weight: 700;
  font-size: 16px;
}

.note {
  color: #999;
  font-size: 12px;
}

.packing .totals {
  width: 100%;
}

.sign {
  margin-top: 32px !important;
  color: #666;
}

@media print {
  .printable {
    padding: 0;
    max-width: none;
  }
}
</style>
