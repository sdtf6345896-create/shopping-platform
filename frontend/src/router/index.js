import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
