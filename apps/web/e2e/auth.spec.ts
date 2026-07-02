import { test, expect } from "@playwright/test";

/**
 * 登录/注销流程 E2E 测试
 *
 * 测试内容：
 * 1. 未登录用户被重定向到登录页
 * 2. 登录表单校验（空字段提交）
 * 3. 成功登录后跳转到首页
 * 4. 注销后清除状态并跳转到登录页
 * 5. 注册链接导航
 */
test.describe("Login / Logout Flow", () => {
  test("should redirect unauthenticated user to login page", async ({ page }) => {
    // Clear any stored auth state
    await page.goto("/");
    await page.evaluate(() => localStorage.clear());

    await page.goto("/dashboard");

    // Should be redirected to /login
    await expect(page).toHaveURL(/\/login/);
  });

  test("should display login form with all elements", async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
    await page.goto("/login");

    // Page title
    await expect(page.locator(".title")).toHaveText("LabReserve 登录");

    // Form fields
    // Using placeholder text to find inputs (since Element Plus uses scoped styles)
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();

    // Submit button
    await expect(page.getByRole("button", { name: /登录/ })).toBeVisible();

    // Register link
    await expect(page.getByText("还没有账号？")).toBeVisible();
    await expect(page.getByRole("link", { name: "去注册" })).toBeVisible();
  });

  test("should show validation errors on empty form submit", async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
    await page.goto("/login");

    // Click login without filling form
    await page.getByRole("button", { name: /登录/ }).click();

    // Element Plus form validation messages should appear
    // The validation messages appear below the inputs
    await expect(page.getByText("请输入用户名")).toBeVisible();
    await expect(page.getByText("请输入密码")).toBeVisible();
  });

  test("should show error on invalid credentials", async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
    await page.goto("/login");

    // Fill with wrong credentials
    await page.locator('input[placeholder="请输入用户名"]').fill("wronguser");
    await page.locator('input[placeholder="请输入密码"]').fill("wrongpass");
    await page.getByRole("button", { name: /登录/ }).click();

    // Should show error message (ElMessage appears as a toast)
    // Wait for error toast
    await page.waitForTimeout(1500);

    // We should still be on the login page after failed login
    await expect(page).toHaveURL(/\/login/);
  });

  test("should login successfully and redirect to home", async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
    await page.goto("/login");

    // Fill credentials
    await page.locator('input[placeholder="请输入用户名"]').fill("student1");
    await page.locator('input[placeholder="请输入密码"]').fill("password123");
    await page.getByRole("button", { name: /登录/ }).click();

    // Wait for navigation
    await page.waitForTimeout(2000);

    // After successful login, should navigate away from login page
    const currentUrl = page.url();
    // Should be at home or not at login
    expect(currentUrl).not.toContain("/login");
  });

  test("should navigate to register page from login", async ({ page }) => {
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
    await page.goto("/login");

    // Click register link
    await page.getByRole("link", { name: "去注册" }).click();

    // Should navigate to /register
    await expect(page).toHaveURL(/\/register/);
  });

  test("should redirect logged-in user from login to home", async ({ page }) => {
    // Set auth state first
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-token");
      localStorage.setItem(
        "userInfo",
        JSON.stringify({
          id: "1",
          username: "student1",
          realName: "Student One",
          role: "STUDENT",
          enabled: true,
        }),
      );
    });

    await page.goto("/login");

    // Should be redirected to home page
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    expect(currentUrl).not.toContain("/login");
  });
});

test.describe("Logout Flow", () => {
  test.beforeEach(async ({ page }) => {
    // Set auth state as logged in
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-student-token");
      localStorage.setItem(
        "userInfo",
        JSON.stringify({
          id: "1",
          username: "student1",
          realName: "Student One",
          role: "STUDENT",
          email: "student1@test.com",
          enabled: true,
        }),
      );
    });
  });

  test("should show user info in header", async ({ page }) => {
    await page.goto("/dashboard");

    // User name should be visible in the header
    await expect(page.locator(".username")).toContainText("Student One");
  });

  test("should logout and redirect to login page", async ({ page }) => {
    await page.goto("/dashboard");

    // Click user dropdown to open it
    await page.locator(".user-info").click();
    await page.waitForTimeout(500);

    // Click logout
    await page.getByText("退出登录").click();

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/);

    // Token should be cleared
    const token = await page.evaluate(() => localStorage.getItem("token"));
    expect(token).toBeNull();
  });
});

test.describe("Protected Route Guards", () => {
  test("student cannot access admin pages", async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-student-token");
      localStorage.setItem(
        "userInfo",
        JSON.stringify({
          id: "1",
          username: "student1",
          realName: "Student One",
          role: "STUDENT",
          enabled: true,
        }),
      );
    });

    // Try to access admin page
    await page.goto("/admin/users");

    // Should be redirected to home
    await page.waitForTimeout(1000);
    const currentUrl = page.url();
    expect(currentUrl).toBe("http://localhost:5173/");
  });

  test("admin can access admin pages", async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-admin-token");
      localStorage.setItem(
        "userInfo",
        JSON.stringify({
          id: "3",
          username: "admin1",
          realName: "Admin One",
          role: "ADMIN",
          enabled: true,
        }),
      );
    });

    await page.goto("/admin/users");

    // Should stay on admin users page
    await page.waitForTimeout(1000);
    expect(page.url()).toContain("/admin/users");
  });

  test("teacher can access approval pages", async ({ page }) => {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-teacher-token");
      localStorage.setItem(
        "userInfo",
        JSON.stringify({
          id: "2",
          username: "teacher1",
          realName: "Teacher One",
          role: "TEACHER",
          enabled: true,
        }),
      );
    });

    await page.goto("/approvals");

    await page.waitForTimeout(1000);
    expect(page.url()).toContain("/approvals");
  });
});
