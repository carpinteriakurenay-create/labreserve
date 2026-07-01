import { describe, it, expect, beforeEach, vi } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useAuthStore } from "@/stores/auth";
import * as authApi from "@/api/auth";
import { mockLoginResponse, mockStudentUser } from "../fixtures";

vi.mock("@/api/auth", () => ({
  login: vi.fn(),
  getMe: vi.fn(),
}));

describe("useAuthStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  describe("login", () => {
    it("should store token and userInfo in localStorage after login", async () => {
      vi.mocked(authApi.login).mockResolvedValue(mockLoginResponse);

      const store = useAuthStore();
      await store.login("student1", "password123");

      expect(store.token).toBe(mockLoginResponse.token);
      expect(store.userInfo).toEqual(mockLoginResponse.user);
      expect(localStorage.getItem("token")).toBe(mockLoginResponse.token);
      expect(localStorage.getItem("userInfo")).toBe(JSON.stringify(mockLoginResponse.user));
    });
  });

  describe("logout", () => {
    it("should clear token and userInfo", async () => {
      vi.mocked(authApi.login).mockResolvedValue(mockLoginResponse);

      const store = useAuthStore();
      await store.login("student1", "password123");
      store.logout();

      expect(store.token).toBeNull();
      expect(store.userInfo).toBeNull();
      expect(localStorage.getItem("token")).toBeNull();
      expect(localStorage.getItem("userInfo")).toBeNull();
    });
  });

  describe("fetchUser", () => {
    it("should fetch and store user info", async () => {
      vi.mocked(authApi.getMe).mockResolvedValue(mockStudentUser);

      const store = useAuthStore();
      await store.fetchUser();

      expect(store.userInfo).toEqual(mockStudentUser);
    });
  });

  describe("computed properties", () => {
    it("isLoggedIn should be false when no token", () => {
      const store = useAuthStore();
      expect(store.isLoggedIn).toBe(false);
    });

    it("isAdmin should reflect user role", async () => {
      vi.mocked(authApi.login).mockResolvedValue({
        ...mockLoginResponse,
        user: { ...mockStudentUser, role: "ADMIN" },
      });

      const store = useAuthStore();
      await store.login("admin1", "password");

      expect(store.isAdmin).toBe(true);
      expect(store.isTeacher).toBe(false);
    });
  });

  describe("localStorage hydration", () => {
    it("should restore token from localStorage", () => {
      localStorage.setItem("token", "stored-token");
      localStorage.setItem("userInfo", JSON.stringify(mockStudentUser));

      const store = useAuthStore();

      expect(store.token).toBe("stored-token");
      expect(store.userInfo).toEqual(mockStudentUser);
    });
  });
});
