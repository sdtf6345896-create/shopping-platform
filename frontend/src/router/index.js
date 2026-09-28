import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAdminAuthStore } from '../stores/adminAuth'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue'), props: true },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { requiresAuth: true } },
  {
    path: '/coupons',
    name: 'CouponCenter',
    component: () => import('../views/CouponCenter.vue'),
    meta: { requiresAuth: true },
  },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { requiresAuth: true } },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../views/member/OrderDetail.vue'),
    props: true,
    meta: { requiresAuth: true },
  },
  {
    path: '/orders/:id/receipt',
    name: 'OrderReceipt',
    component: () => import('../views/PrintOrder.vue'),
    props: (route) => ({ id: route.params.id, variant: 'receipt' }),
    meta: { requiresAuth: true },
  },
  // 揀貨單放在後台 layout 外面,列印時才不會帶到側邊選單
  {
    path: '/admin/orders/:id/packing-slip',
    name: 'AdminPackingSlip',
    component: () => import('../views/PrintOrder.vue'),
    props: (route) => ({ id: route.params.id, variant: 'packing' }),
    meta: { requiresAdminAuth: true },
  },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('../views/ForgotPassword.vue') },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPassword.vue') },
  { path: '/verify-email', name: 'VerifyEmail', component: () => import('../views/VerifyEmail.vue') },
  {
    path: '/member',
    component: () => import('../views/member/MemberLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'MemberProfile' } },
      { path: 'profile', name: 'MemberProfile', component: () => import('../views/member/Profile.vue') },
      { path: 'addresses', name: 'MemberAddresses', component: () => import('../views/member/Addresses.vue') },
      { path: 'orders', name: 'MemberOrders', component: () => import('../views/member/OrderList.vue') },
      { path: 'wishlist', name: 'MemberWishlist', component: () => import('../views/member/Wishlist.vue') },
      { path: 'points', name: 'MemberPoints', component: () => import('../views/member/Points.vue') },
      { path: 'coupons', name: 'MemberCoupons', component: () => import('../views/member/MyCoupons.vue') },
      { path: 'referral', name: 'MemberReferral', component: () => import('../views/member/Referral.vue') },
      {
        path: 'notifications',
        name: 'MemberNotifications',
        component: () => import('../views/member/Notifications.vue'),
      },
      {
        path: 'browsing-history',
        name: 'MemberBrowsingHistory',
        component: () => import('../views/member/BrowsingHistory.vue'),
      },
    ],
  },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('../views/admin/AdminLogin.vue') },
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
      { path: 'coupons', name: 'AdminCouponList', component: () => import('../views/admin/CouponList.vue') },
      {
        path: 'order-messages',
        name: 'AdminOrderMessageList',
        component: () => import('../views/admin/OrderMessageList.vue'),
      },
      { path: 'questions', name: 'AdminQuestionList', component: () => import('../views/admin/QuestionList.vue') },
      { path: 'returns', name: 'AdminReturnList', component: () => import('../views/admin/ReturnList.vue') },
      { path: 'audit-logs', name: 'AdminAuditLogList', component: () => import('../views/admin/AuditLogList.vue') },
    ],
  },
  // 其他未定義的網址一律顯示 404(需放在最後)
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
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

export default router
