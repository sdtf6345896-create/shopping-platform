import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAdminAuthStore } from '../stores/adminAuth'
import { setMetaDescription, setPageTitle } from '../utils/pageMeta'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/products', name: 'ProductList', component: () => import('../views/ProductList.vue'), meta: { title: '全部商品' } },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue'), props: true },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { title: '購物車', requiresAuth: true } },
  {
    path: '/coupons',
    name: 'CouponCenter',
    component: () => import('../views/CouponCenter.vue'),
    meta: { title: '領券中心', requiresAuth: true },
  },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { title: '結帳', requiresAuth: true } },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../views/member/OrderDetail.vue'),
    props: true,
    meta: { title: '訂單詳情', requiresAuth: true },
  },
  {
    path: '/orders/:id/receipt',
    name: 'OrderReceipt',
    component: () => import('../views/PrintOrder.vue'),
    props: (route) => ({ id: route.params.id, variant: 'receipt' }),
    meta: { title: '訂單收據', requiresAuth: true },
  },
  // 揀貨單放在後台 layout 外面,列印時才不會帶到側邊選單
  {
    path: '/admin/orders/:id/packing-slip',
    name: 'AdminPackingSlip',
    component: () => import('../views/PrintOrder.vue'),
    props: (route) => ({ id: route.params.id, variant: 'packing' }),
    meta: { requiresAdminAuth: true },
  },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '會員登入' } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { title: '註冊會員' } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('../views/ForgotPassword.vue'), meta: { title: '忘記密碼' } },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPassword.vue'), meta: { title: '重設密碼' } },
  { path: '/verify-email', name: 'VerifyEmail', component: () => import('../views/VerifyEmail.vue'), meta: { title: '驗證 Email' } },
  {
    path: '/member',
    component: () => import('../views/member/MemberLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'MemberProfile' } },
      { path: 'profile', name: 'MemberProfile', component: () => import('../views/member/Profile.vue'), meta: { title: '個人資料' } },
      { path: 'addresses', name: 'MemberAddresses', component: () => import('../views/member/Addresses.vue'), meta: { title: '收件地址' } },
      { path: 'orders', name: 'MemberOrders', component: () => import('../views/member/OrderList.vue'), meta: { title: '我的訂單' } },
      { path: 'wishlist', name: 'MemberWishlist', component: () => import('../views/member/Wishlist.vue'), meta: { title: '我的收藏' } },
      { path: 'points', name: 'MemberPoints', component: () => import('../views/member/Points.vue'), meta: { title: '我的購物金' } },
      { path: 'coupons', name: 'MemberCoupons', component: () => import('../views/member/MyCoupons.vue'), meta: { title: '我的優惠券' } },
      { path: 'referral', name: 'MemberReferral', component: () => import('../views/member/Referral.vue'), meta: { title: '邀請好友' } },
      {
        path: 'notifications',
        name: 'MemberNotifications',
        meta: { title: '通知中心' },
        component: () => import('../views/member/Notifications.vue'),
      },
      {
        path: 'browsing-history',
        name: 'MemberBrowsingHistory',
        meta: { title: '瀏覽紀錄' },
        component: () => import('../views/member/BrowsingHistory.vue'),
      },
    ],
  },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('../views/admin/AdminLogin.vue'), meta: { title: '後台登入' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdminAuth: true },
    children: [
      { path: '', redirect: { name: 'AdminDashboard' } },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'products', name: 'AdminProductList', component: () => import('../views/admin/ProductList.vue') },
      { path: 'products/new', name: 'AdminProductCreate', component: () => import('../views/admin/ProductForm.vue') },
      {
        path: 'products/:id/edit',
        name: 'AdminProductEdit',
        component: () => import('../views/admin/ProductForm.vue'),
        props: true,
      },
      { path: 'banners', name: 'AdminBannerList', component: () => import('../views/admin/BannerList.vue') },
      { path: 'categories', name: 'AdminCategoryList', component: () => import('../views/admin/CategoryList.vue') },
      { path: 'orders', name: 'AdminOrderList', component: () => import('../views/admin/OrderList.vue') },
      {
        path: 'orders/:id',
        name: 'AdminOrderDetail',
        component: () => import('../views/admin/OrderDetail.vue'),
        props: true,
      },
      { path: 'reports', name: 'AdminReport', component: () => import('../views/admin/Report.vue') },
      { path: 'members', name: 'AdminMemberList', component: () => import('../views/admin/MemberList.vue') },
      {
        path: 'members/:id',
        name: 'AdminMemberDetail',
        component: () => import('../views/admin/MemberDetail.vue'),
        props: true,
      },
      { path: 'promotions', name: 'AdminPromotionList', component: () => import('../views/admin/PromotionList.vue') },
      { path: 'coupons', name: 'AdminCouponList', component: () => import('../views/admin/CouponList.vue') },
      {
        path: 'order-messages',
        name: 'AdminOrderMessageList',
        component: () => import('../views/admin/OrderMessageList.vue'),
      },
      { path: 'reviews', name: 'AdminReviewList', component: () => import('../views/admin/ReviewList.vue') },
      { path: 'questions', name: 'AdminQuestionList', component: () => import('../views/admin/QuestionList.vue') },
      { path: 'returns', name: 'AdminReturnList', component: () => import('../views/admin/ReturnList.vue') },
      { path: 'audit-logs', name: 'AdminAuditLogList', component: () => import('../views/admin/AuditLogList.vue') },
    ],
  },
  // 其他未定義的網址一律顯示 404(需放在最後)
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue'), meta: { title: '找不到頁面' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (to.meta.requiresAdminAuth) {
    const adminAuthStore = useAdminAuthStore()
    if (!adminAuthStore.isLoggedIn) {
      return { name: 'AdminLogin', query: { redirect: to.fullPath } }
    }
    return true
  }

  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  return true
})

// 分頁標題與預設描述;商品頁載入後會再以商品名稱與說明覆寫
router.afterEach((to) => {
  setPageTitle(to.meta.title || (to.path.startsWith('/admin') ? '管理後台' : null))
  setMetaDescription(null)
})

export default router
