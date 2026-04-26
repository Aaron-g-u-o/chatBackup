import { createRouter, createWebHistory } from 'vue-router'
import createGuard from './guard'

const HomeView = () => import(/* webpackChunkName: "Home" */ '@/views/Home/index.vue')
const HomeChatView = () => import(/* webpackChunkName: "Home" */ '@/views/Home/Chat/index.vue')
const HomeContactsView = () =>
  import(/* webpackChunkName: "Home" */ '@/views/Home/Contacts/index.vue')
const GuildView = () =>
  import(/* webpackChunkName: "Guild" */ '@/views/Home/Guild/index.vue')
const DiscoveryView = () =>
  import(/* webpackChunkName: "Discovery" */ '@/views/Home/Discovery/index.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      children: [
        {
          path: '',
          name: 'chat',
          component: HomeChatView,
        },
        {
          path: 'contact',
          name: 'contact',
          component: HomeContactsView,
        },
        {
          path: 'guild',
          name: 'guild',
          component: GuildView,
        },
        {
          path: 'discovery',
          name: 'discovery',
          component: DiscoveryView,
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

createGuard(router)

export default router
