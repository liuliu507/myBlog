<template>
  <AuthCard title="重置密码" max-width="420px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="注册邮箱" size="large" />
      </el-form-item>

      <el-form-item prop="code">
        <div class="code-row">
          <el-input v-model="form.code" placeholder="6 位验证码" size="large" />
          <el-button
            size="large"
            :disabled="countdown > 0 || sending"
            :loading="sending"
            @click="onSendCode"
          >
            {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="新密码（6-32 位）"
          size="large"
          show-password
        />
      </el-form-item>

      <el-form-item prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="确认新密码"
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
        重置密码
      </el-button>
    </el-form>

    <template #footer>
      <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
    </template>
  </AuthCard>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/auth'
import AuthCard from '@/components/auth/AuthCard.vue'
import {
  emailRules,
  createPasswordRules,
  createConfirmPasswordRules,
} from '@/utils/validation'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const rules = {
  email: emailRules,
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位', trigger: 'blur' },
  ],
  newPassword: createPasswordRules('请输入新密码'),
  confirmPassword: createConfirmPasswordRules(() => form.newPassword),
}

async function onSendCode() {
  // 单独校验 email
  await formRef.value.validateField('email')
  sending.value = true
  try {
    await sendResetCode({ email: form.email })
    ElMessage.success('验证码已发送，请查收邮件')
    // 启动 60 秒倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await resetPassword({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功，请用新密码登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>
