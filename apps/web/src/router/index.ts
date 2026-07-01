import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: () => import("@/views/LoginView.vue"),
    },
    {
      path: "/register",
      name: "register",
      component: () => import("@/views/RegisterView.vue"),
    },
    {
      path: "/",
      component: () => import("@/views/AppLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        {
          path: "",
          name: "home",
          component: () => import("@/views/Home.vue"),
        },
        {
          path: "labs",
          name: "labs",
          component: () => import("@/views/LabsView.vue"),
        },
        {
          path: "bookings/create",
          name: "bookingCreate",
          component: () => import("@/views/BookingCreateView.vue"),
        },
        {
          path: "bookings/mine",
          name: "myBookings",
          component: () => import("@/views/MyBookingsView.vue"),
        },
        {
          path: "approvals",
          name: "pendingApprovals",
          component: () => import("@/views/PendingApprovalsView.vue"),
          meta: { roles: ["TEACHER", "ADMIN"] },
        },
        {
          path: "admin/labs",
          name: "adminLabs",
          component: () => import("@/views/AdminLabsView.vue"),
          meta: { roles: ["ADMIN"] },
        },
        {
          path: "labs/:id",
          name: "labDetail",
          component: () => import("@/views/LabDetailView.vue"),
        },
        {
          path: "admin/users",
          name: "adminUsers",
          component: () => import("@/views/AdminUsersView.vue"),
          meta: { roles: ["ADMIN"] },
        },
        {
          path: "admin/equipment",
          name: "adminEquipment",
          component: () => import("@/views/AdminEquipmentView.vue"),
          meta: { roles: ["ADMIN"] },
        },
        {
          path: "borrows/request",
          name: "borrowRequest",
          component: () => import("@/views/BorrowRequestView.vue"),
        },
        {
          path: "borrows/mine",
          name: "myBorrows",
          component: () => import("@/views/MyBorrowsView.vue"),
        },
        {
          path: "admin/borrows",
          name: "adminBorrows",
          component: () => import("@/views/AdminBorrowsView.vue"),
          meta: { roles: ["TEACHER", "ADMIN"] },
        },
        {
          path: "admin/courses",
          name: "adminCourses",
          component: () => import("@/views/AdminCoursesView.vue"),
          meta: { roles: ["ADMIN"] },
        },
        {
          path: "schedule",
          name: "mySchedule",
          component: () => import("@/views/MyScheduleView.vue"),
        },
        {
          path: "notices",
          name: "notices",
          component: () => import("@/views/NoticesView.vue"),
        },
        {
          path: "reviews/mine",
          name: "myReviews",
          component: () => import("@/views/MyReviewsView.vue"),
        },
        {
          path: "dashboard",
          name: "dashboard",
          component: () => import("@/views/DashboardView.vue"),
        },
        {
          path: "admin/usage-records",
          name: "usageRecords",
          component: () => import("@/views/UsageRecordsView.vue"),
          meta: { roles: ["TEACHER", "ADMIN"] },
        },
      ],
    },
  ],
});

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore();

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

export default router;
