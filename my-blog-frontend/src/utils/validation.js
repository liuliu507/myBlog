// 认证相关表单校验规则预设，供登录/注册/重置密码页面复用

export const emailRules = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
]

export function createPasswordRules(requiredMessage = '请输入密码') {
  return [
    { required: true, message: requiredMessage, trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ]
}

export const passwordRules = createPasswordRules()

// getPassword: 返回需要比对的原密码的函数
export function createConfirmPasswordRules(getPassword) {
  const validateConfirm = (rule, value, callback) => {
    if (value !== getPassword()) {
      callback(new Error('两次密码不一致'))
    } else {
      callback()
    }
  }

  return [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ]
}
