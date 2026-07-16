<template>
  <section class="login-page">
    <div class="login-panel">
      <div class="login-copy">
        <p class="eyebrow">{{ isAdmin ? '管理后台' : '用户客服端' }}</p>
        <h1>{{ isAdmin ? '管理员登录' : '用户登录' }}</h1>
        <p class="hint">
          {{ isAdmin ? '进入文档管理、工单处理和客服运营后台。' : '登录后开始咨询商品、发货、退款和售后问题。' }}
        </p>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password autocomplete="current-password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :icon="Right" @click="submit">登录</el-button>
      </el-form>

      <div class="demo-account">
        演示账号：{{ isAdmin ? 'admin / admin123' : 'user / 123456' }}
      </div>
      <router-link class="switch-link" :to="isAdmin ? '/user/login' : '/admin/login'">
        {{ isAdmin ? '去用户客服端登录' : '去管理后台登录' }}
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Right } from '@element-plus/icons-vue'
import { login, type UserRole } from '../auth'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')

const role = computed(() => (route.meta.loginRole as UserRole) ?? 'customer')
const isAdmin = computed(() => role.value === 'admin')

watch(role, () => {
  username.value = isAdmin.value ? 'admin' : 'user'
  password.value = isAdmin.value ? 'admin123' : '123456'
}, { immediate: true })

function submit() {
  const user = login(role.value, username.value, password.value)
  if (!user) {
    ElMessage.error('账号或密码不正确')
    return
  }
  router.push(user.role === 'admin' ? '/admin' : '/chat')
}
</script>
