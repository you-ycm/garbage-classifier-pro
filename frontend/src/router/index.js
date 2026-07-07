// ===== router/index.js =====

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/userStore'

// ---------- 导入组件（注意区分是 views 还是 components） ----------
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ForgotPassword from '../views/ForgotPassword.vue'

// 登录后显示的主页组件
import GarbageClassifier from '../components/GarbageClassifier.vue'
import History from '../views/History.vue'
import AdminPanel from '../components/AdminPanel.vue'
import Profile from '../views/Profile.vue'
import Favorites from '../views/Favorites.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/register',
        name: 'Register',
        component: Register
    },
    {
        path: '/forgot-password',
        name: 'ForgotPassword',
        component: ForgotPassword
    },
    // ---------- 登录后的主页面路由 ----------
    {
        path: '/classify',
        name: 'Classify',
        component: GarbageClassifier
    },
    {
        path: '/history',
        name: 'History',
        component: History
    },
    {
        path: '/admin',
        name: 'Admin',
        component: AdminPanel
    },
    {
        path: '/profile',
        name: 'Profile',
        component: Profile
    },
    {
        path: '/favorites',
        name: 'Favorites',
        component: Favorites
    },
    {
        path: '/knowledge',
        name: 'Knowledge',
        component: KnowledgeBase
    },
    // 默认重定向到垃圾分类页面
    {
        path: '/',
        redirect: '/classify'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：拦截未登录的访问
router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const token = userStore.token
    const publicPages = ['/login', '/register', '/forgot-password']

    if (publicPages.includes(to.path)) {
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