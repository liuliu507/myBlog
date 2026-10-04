<template>
  <AuthCard title="登录">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱" size="large" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          size="large"
          show-password
          @keyup.enter="onSubmit"
        />
      </el-form-item>

      <div class="forgot">
        <el-link type="primary" @click="$router.push('/forgot-password')">
          忘记密码？
        </el-link>
      </div>

      <el-button
        type="primary"
        size="large"
        style="width: 100%"
        :loading="loading"
        @click="onSubmit"
      >
        登录
      </el-button>
    </el-form>

    <template #footer>
      还没有账号？
      <el-link type="primary" @click="$router.push('/register')">去注册</el-link>
    </template>
  </AuthCard>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AuthCard from '@/components/auth/AuthCard.vue'
import { emailRules } from '@/utils/validation'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
})

const rules = {
  email: emailRules,
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ email: form.email, password: form.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // request.js 里已经弹了错误，这里不用重复
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot {
  text-align: right;
  margin-bottom: 12px;
}
</style>
