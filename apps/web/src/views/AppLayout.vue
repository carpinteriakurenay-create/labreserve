<script setup lang="ts">
import { computed } from "vue";
import type { Component } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  HomeFilled,
  UserFilled,
  OfficeBuilding,
  Setting,
  Tickets,
  Checked,
  Monitor,
  ShoppingCart,
  Bell,
  Notebook,
  Reading,
  Star,
  DataAnalysis,
  Document,
  User,
  WarnTriangleFilled,
} from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

interface MenuItem {
  index: string;
  title: string;
  icon: Component;
  roles?: string[];
}

const menuItems: MenuItem[] = [
  { index: "/", title: "系统首页", icon: HomeFilled },
  { index: "/labs", title: "实验室列表", icon: OfficeBuilding },
  { index: "/bookings/mine", title: "我的预约", icon: Tickets },
  { index: "/borrows/request", title: "借用申请", icon: ShoppingCart },
  { index: "/borrows/mine", title: "我的借用", icon: Tickets },
  { index: "/approvals", title: "预约审批", icon: Checked, roles: ["TEACHER", "ADMIN"] },
  { index: "/admin/borrows", title: "借用管理", icon: ShoppingCart, roles: ["TEACHER", "ADMIN"] },
  { index: "/admin/users", title: "用户管理", icon: UserFilled, roles: ["ADMIN"] },
  { index: "/admin/labs", title: "实验室管理", icon: Setting, roles: ["ADMIN"] },
  { index: "/admin/equipment", title: "设备管理", icon: Monitor, roles: ["ADMIN"] },
  { index: "/notices", title: "通知公告", icon: Bell },
  { index: "/schedule", title: "我的课表", icon: Notebook },
  { index: "/admin/courses", title: "课程管理", icon: Reading, roles: ["ADMIN"] },
  { index: "/dashboard", title: "数据仪表盘", icon: DataAnalysis },
  { index: "/reviews/mine", title: "我的评价", icon: Star },
  { index: "/admin/usage-records", title: "使用记录", icon: Document, roles: ["TEACHER", "ADMIN"] },
  { index: "/admin/students", title: "学生信息", icon: User, roles: ["ADMIN"] },
  { index: "/admin/repair-logs", title: "报修记录", icon: WarnTriangleFilled, roles: ["ADMIN"] },
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
