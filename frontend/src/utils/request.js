// src/utils/request.js
import axios from 'axios'

// 后端地址：改成你虚拟机的 IP
const BASE_URL = 'http://192.168.58.128:8080/api'

// 创建 axios 实例
const request = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
})

// 请求拦截器（自动带上 token）
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器（处理错误）
request.interceptors.response.use(
    (response) => {
        return response.data
    },
    (error) => {
        if (error.response && error.response.status === 401) {
            // token 过期，跳转到登录页
            localStorage.removeItem('token')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default request