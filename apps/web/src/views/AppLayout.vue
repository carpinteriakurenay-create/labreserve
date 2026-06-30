<script setup lang="ts">
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { HomeFilled, UserFilled } from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

interface MenuItem {
  index: string;
  title: string;
  icon: typeof HomeFilled;
  roles?: string[];
}

const menuItems: MenuItem[] = [
  { index: "/", title: "系统首页", icon: HomeFilled },
  { index: "/admin/users", title: "用户管理", icon: UserFilled, roles: ["ADMIN"] },
];

const visibleMenuItems = computed(() =>
  menuItems.filter(
    (item) => !item.roles || (authStore.userRole && item.roles.includes(authStore.userRole)),
  ),
);

const activeMenu = computed(() => route.path);

function handleMenuSelect(index: string) {
  router.push(index);
}

function handleLogout() {
  authStore.logout();
  router.push("/login");
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">LabReserve</span>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{
              authStore.userInfo?.realName ?? authStore.userInfo?.username
            }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px">
        <el-menu
          :default-active="activeMenu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item v-for="item in visibleMenuItems" :key="item.index" :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.logo {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
}

.username {
  font-size: 14px;
}

.el-aside {
  background: #304156;
  overflow: hidden;
}

.el-menu {
  border-right: none;
}

.main-content {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 0;
}
</style>
