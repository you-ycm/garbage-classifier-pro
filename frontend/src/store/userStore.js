// src/store/userStore.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const role = ref(localStorage.getItem('role') || '')
    const username = ref(localStorage.getItem('username') || '')
    const email = ref(localStorage.getItem('email') || '')

    function setUserInfo(newToken, newRole, newUsername, newEmail = '') {
        token.value = newToken
        role.value = newRole
        username.value = newUsername
        email.value = newEmail
        localStorage.setItem('token', newToken)
        localStorage.setItem('role', newRole)
        localStorage.setItem('username', newUsername)
        localStorage.setItem('email', newEmail)
    }

    function logout() {
        token.value = ''
        role.value = ''
        username.value = ''
        email.value = ''
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('username')
        localStorage.removeItem('email')
    }

    return { token, role, username, email, setUserInfo, logout }
})