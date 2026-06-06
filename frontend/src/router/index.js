import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/market',
    name: 'Market',
    component: () => import('@/views/Market.vue')
  },
  {
    path: '/publish',
    name: 'Publish',
    component: () => import('@/views/Publish.vue')
  },
  {
    path: '/my',
    name: 'My',
    component: () => import('@/views/My.vue')
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: () => import('@/views/Detail.vue')
  },
  {
    path: '/offers',
    name: 'Offers',
    component: () => import('@/views/Offers.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
