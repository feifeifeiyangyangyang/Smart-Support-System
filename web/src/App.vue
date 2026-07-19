<template>
  <el-container class="shell">
    <el-aside v-if="user && !isLoginRoute" width="224px" class="aside">
      <div class="brand">智服通</div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item v-if="user.role === 'customer'" index="/chat">客服咨询</el-menu-item>
        <el-menu-item v-if="user.role === 'admin'" index="/admin">管理后台</el-menu-item>
      </el-menu>
      <div class="health" :class="{ ok: healthOk }">
        <span class="health-dot" aria-hidden="true"></span>
        后端：{{ healthText }}
      </div>
    </el-aside>
    <el-main class="main" :class="{ centered: !user || isLoginRoute }">
      <div v-if="user && !isLoginRoute" class="topbar">
        <div class="topbar-actions">
          <div class="topbar-user">
            <strong>{{ user.name }}</strong>
            <span>{{ user.role === 'admin' ? '管理员账号' : '用户账号' }}</span>
          </div>
          <el-button type="danger" plain :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { api, unwrap } from './api'
import { currentUser, logout, type SessionUser } from './auth'

const route = useRoute()
const router = useRouter()
const user = ref<SessionUser | null>(currentUser())
const healthText = ref('检查中')
const healthOk = ref(false)
const isLoginRoute = computed(() => route.path === '/user/login' || route.path === '/admin/login')

watch(() => route.fullPath, () => {
  user.value = currentUser()
})

onMounted(async () => {
  await checkHealth()
})

async function checkHealth() {
  try {
    const data = await unwrap<Record<string, unknown>>(api.get('/health'))
    healthOk.value = data.status === 'UP'
    healthText.value = healthOk.value ? '已连接' : '异常'
  } catch {
    healthText.value = '未连接'
  }
}

async function handleLogout() {
  const role = user.value?.role
  try {
    await api.post('/auth/logout')
  } catch {
    // Local cleanup still matters if the token has already expired.
  }
  logout()
  user.value = null
  router.push(role === 'admin' ? '/admin/login' : '/user/login')
}
</script>
