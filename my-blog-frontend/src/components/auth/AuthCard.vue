<template>
  <div class="auth-page">
    <el-card class="auth-card" :style="{ maxWidth }">
      <h2 class="title">{{ title }}</h2>
      <slot />
      <div v-if="$slots.footer" class="footer">
        <slot name="footer" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  maxWidth: {
    type: String,
    default: '400px',
  },
})
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
}
.auth-card {
  width: 100%;
  padding: 12px;
}
.title {
  text-align: center;
  margin-bottom: 24px;
}
.footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}

@media (max-width: 767px) {
  .auth-card {
    box-sizing: border-box;
  }
  :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>
