import { describe, it, expect, beforeEach, vi } from "vitest";
import { createRouter, createWebHistory, type Router, type RouteComponent } from "vue-router";
import { setActivePinia, createPinia } from "pinia";
import { useAuthStore } from "@/stores/auth";

const MockComponent: RouteComponent = { template: "<div></div>" };

const mockFetchUser = vi.fn();
vi.mock("@/api/auth", () => ({
  getMe: vi.fn(() => mockFetchUser()),
}));

function createTestRouter(): Router {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: "/login", name: "login", component: MockComponent },
      {
        path: "/",
        component: MockComponent,
        meta: { requiresAuth: true },
        children: [
          { path: "", name: "home", component: MockComponent },
          {
            path: "admin/labs",
            name: "adminLabs",
            component: MockComponent,
            meta: { roles: ["ADMIN"] },
          },
          {
            path: "admin/users",
            name: "adminUsers",
            component: MockComponent,
            meta: { roles: ["ADMIN"] },
          },
          {
            path: "approvals",
            name: "pendingApprovals",
            component: MockComponent,
            meta: { roles: ["TEACHER", "ADMIN"] },
          },
          {
            path: "dashboard",
            name: "dashboard",
            component: MockComponent,
          },
        ],
      },
    ],
  });
}

describe("Router Guards", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  function addBeforeEach(router: Router) {
    const authStore = useAuthStore();
    router.beforeEach(async (to, _from, next) => {
      if (authStore.token && !authStore.userInfo) {
        try {
          await authStore.fetchUser();
        } catch {
          authStore.logout();
          next({ path: "/login", query: { redirect: to.fullPath } });
          return;
        }
      }

      if (to.matched.some((r) => r.meta.requiresAuth) && !authStore.isLoggedIn) {
        next({ path: "/login", query: { redirect: to.fullPath } });
        return;
      }

      if (to.meta.roles) {
        const requiredRoles = to.meta.roles as string[];
        if (!authStore.userRole || !requiredRoles.includes(authStore.userRole)) {
          next("/");
          return;
        }
      }

      if (to.path === "/login" && authStore.isLoggedIn) {
        next("/");
        return;
      }

      next();
    });
  }

  it("should redirect unauthenticated user to /login", async () => {
    const router = createTestRouter();
    addBeforeEach(router);
    router.push("/dashboard");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/login");
  });

  it("should redirect student from /admin/labs to /", async () => {
    localStorage.setItem("token", "test-token");
    localStorage.setItem(
      "userInfo",
      JSON.stringify({ role: "STUDENT", id: "1", username: "s1", realName: "S" }),
    );

    const router = createTestRouter();
    addBeforeEach(router);
    router.push("/admin/labs");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/");
  });

  it("should allow admin to access /admin/labs", async () => {
    localStorage.setItem("token", "test-token");
    localStorage.setItem(
      "userInfo",
      JSON.stringify({ role: "ADMIN", id: "3", username: "a1", realName: "A" }),
    );

    const router = createTestRouter();
    addBeforeEach(router);
    router.push("/admin/labs");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/admin/labs");
  });

  it("should redirect logged-in user from /login to /", async () => {
    localStorage.setItem("token", "test-token");
    localStorage.setItem(
      "userInfo",
      JSON.stringify({ role: "STUDENT", id: "1", username: "s1", realName: "S" }),
    );

    const router = createTestRouter();
    addBeforeEach(router);
    router.push("/login");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/");
  });
});
