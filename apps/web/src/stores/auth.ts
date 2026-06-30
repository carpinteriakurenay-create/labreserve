import { defineStore } from "pinia";
import { ref, computed } from "vue";
import type { UserInfo } from "@labreserve/shared";
import * as authApi from "@/api/auth";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string | null>(localStorage.getItem("token"));
  const userInfo = ref<UserInfo | null>(null);

  const stored = localStorage.getItem("userInfo");
  if (stored) {
    try {
      userInfo.value = JSON.parse(stored);
    } catch {
      localStorage.removeItem("userInfo");
    }
  }

  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => userInfo.value?.role === "ADMIN");
  const isTeacher = computed(() => userInfo.value?.role === "TEACHER");
  const userRole = computed(() => userInfo.value?.role ?? null);

  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password });
    token.value = result.token;
    userInfo.value = result.user;
    localStorage.setItem("token", result.token);
    localStorage.setItem("userInfo", JSON.stringify(result.user));
  }

  function logout() {
    token.value = null;
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  }

  async function fetchUser() {
    const user = await authApi.getMe();
    userInfo.value = user;
    localStorage.setItem("userInfo", JSON.stringify(user));
  }

  return { token, userInfo, isLoggedIn, isAdmin, isTeacher, userRole, login, logout, fetchUser };
});
