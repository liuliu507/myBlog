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
  min-height: calc(100vh - 164px);
  padding: 24px 0;
}
.auth-card {
  width: 100%;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 16px 48px rgb(32 42 37 / 7%);
}
.title {
  text-align: center;
  margin: 4px 0 26px;
  color: var(--ink);
  font-size: 26px;
  font-weight: 720;
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
    padding: 12px;
  }
  :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>
