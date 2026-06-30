<script setup lang="ts">
import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import { USER_ROLE_LABELS } from "@labreserve/shared";

const authStore = useAuthStore();

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return "上午好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const roleLabel = computed(() => {
  const role = authStore.userRole;
  return role ? (USER_ROLE_LABELS[role as keyof typeof USER_ROLE_LABELS] ?? role) : "";
});
</script>

<template>
  <div class="home">
    <div class="welcome-card">
      <h1>{{ greeting }}，{{ authStore.userInfo?.realName ?? authStore.userInfo?.username }}</h1>
      <p class="role-tag">当前角色：{{ roleLabel }}</p>
      <p class="subtitle">欢迎使用 LabReserve 实验室预约管理系统</p>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding: 32px;
}

.welcome-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.welcome-card h1 {
  margin: 0 0 12px;
  font-size: 28px;
  color: #303133;
}

.role-tag {
  color: var(--el-color-primary);
  font-size: 14px;
  margin: 0 0 8px;
}

.subtitle {
  color: #909399;
  margin: 0;
}
</style>
