import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'index',
      component: () => import('./views/MainView.vue'),
    },
    {
      path: '/RK2023',
      name: 'RK2023',
      component: () => import('./views/MainView.vue'),
    },
    {
      path: '/KOV2025',
      name: 'KOV2025',
      component: () => import('./views/MainView.vue'),
    },
    {
      path: '/KOV2021',
      name: 'KOV2021',
      component: () => import('./views/MainView.vue'),
    },
  ],
})

export default router
