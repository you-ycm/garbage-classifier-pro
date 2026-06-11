// src/store/userStore.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const role = ref(localStorage.getItem('role') || '')
    const username = ref(localStorage.getItem('username') || '')

    function setUserInfo(newToken, newRole, newUsername) {
        token.value = newToken
        role.value = newRole
        username.value = newUsername
        localStorage.setItem('token', newToken)
        localStorage.setItem('role', newRole)
        localStorage.setItem('username', newUsername)
    }

    function logout() {
        token.value = ''
        role.value = ''
        username.value = ''
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('username')
    }

    return { token, role, username, setUserInfo, logout }
})