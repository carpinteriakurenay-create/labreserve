<script setup lang="ts">
const message = "LabReserve 实验室预约管理系统";
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <h1>{{ message }}</h1>
      <p class="subtitle">高校实验室预约管理系统</p>
    </el-header>
    <el-main>
      <el-card>
        <p>欢迎使用 LabReserve 实验室预约管理系统</p>
      </el-card>
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.header h1 {
  margin: 0;
  font-size: 28px;
}
.subtitle {
  margin: 8px 0 0;
  opacity: 0.85;
}
</style>
