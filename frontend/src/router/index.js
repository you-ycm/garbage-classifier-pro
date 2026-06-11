// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/userStore'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue')
    },
    {
        path: '/',
        component: () => import('../App.vue'),
        redirect: '/classify'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：未登录强制跳转登录
router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const token = userStore.token

    // 允许访问登录页和注册页
    if (to.path === '/login' || to.path === '/register') {
        next()
        return
    }

    if (!token) {
        next('/login')
    } else {
        next()
    }
})

export default router