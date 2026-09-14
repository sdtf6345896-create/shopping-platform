import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useAdminAuthStore } from '../stores/adminAuth'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
  { path: '/products/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue'), props: true },
  { path: '/cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { requiresAuth: true } },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { requiresAuth: true } },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../views/member/OrderDetail.vue'),
    props: true,
    meta: { requiresAuth: true },
  },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/member',
    component: () => import('../views/member/MemberLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: { name: 'MemberProfile' } },
      { path: 'profile', name: 'MemberProfile', component: () => import('../views/member/Profile.vue') },
      { path: 'addresses', name: 'MemberAddresses', component: () => import('../views/member/Addresses.vue') },
      { path: 'orders', name: 'MemberOrders', component: () => import('../views/member/OrderList.vue') },
    ],
  },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('../views/admin/AdminLogin.vue') },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdminAuth: true },
    children: [
      { path: '', redirect: { name: 'AdminProductList' } },
      { path: 'products', name: 'AdminProductList', component: () => import('../views/admin/ProductList.vue') },
      { path: 'products/new', name: 'AdminProductCreate', component: () => import('../views/admin/ProductForm.vue') },
      {
        path: 'products/:id/edit',
        name: 'AdminProductEdit',
        component: () => import('../views/admin/ProductForm.vue'),
        props: true,
      },
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
    ],
  },
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
