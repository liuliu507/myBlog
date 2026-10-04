<template>
  <AuthCard title="注册">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱" size="large" />
      </el-form-item>
      <el-form-item prop="nickname">
        <el-input v-model="form.nickname" placeholder="昵称" size="large" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码（6-32 位）"
          size="large"
          show-password
        />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="确认密码"
          size="large"
          show-password
          @keyup.enter="onSubmit"
        />
      </el-form-item>
      <el-button
        type="primary"
        size="large"
        style="width: 100%"
        :loading="loading"
        @click="onSubmit"
      >
        注册
      </el-button>
    </el-form>

    <template #footer>
      已有账号？
      <el-link type="primary" @click="$router.push('/login')">去登录</el-link>
    </template>
  </AuthCard>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import AuthCard from '@/components/auth/AuthCard.vue'
import {
  emailRules,
  passwordRules,
  createConfirmPasswordRules,
} from '@/utils/validation'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const rules = {
  email: emailRules,
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称最多 20 个字符', trigger: 'blur' },
  ],
  password: passwordRules,
  confirmPassword: createConfirmPasswordRules(() => form.password),
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      email: form.email,
      nickname: form.nickname,
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // request.js 已经弹了错误
  } finally {
    loading.value = false
  }
}
</script>
