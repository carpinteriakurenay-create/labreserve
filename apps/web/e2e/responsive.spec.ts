import { test, expect } from "@playwright/test";

/**
 * 响应式布局 E2E 测试
 *
 * 测试不同设备和视口尺寸下的布局表现：
 * 1. 桌面端完整布局
 * 2. 平板端适配
 * 3. 手机端适配
 * 4. 登录页适配
 */
test.describe("Responsive Layout", () => {
  // Helper to set auth state
  async function loginAsStudent(page: import("@playwright/test").Page) {
    await page.goto("/");
    await page.evaluate(() => {
      localStorage.setItem("token", "e2e-test-responsive-token");
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
  }

  test.describe("Desktop layout (1280x800)", () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test("should show sidebar and header", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // Header should be visible
      await expect(page.locator(".header")).toBeVisible();
      await expect(page.locator(".logo")).toHaveText("LabReserve");

      // Sidebar should be visible
      const aside = page.locator(".el-aside");
      await expect(aside).toBeVisible();
    });

    test("KPI cards should be in 4-column grid on desktop", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // KPI grid should be 4 columns
      const kpiRow = page.locator(".kpi-row");
      const gridColumns = await kpiRow.evaluate(
        (el) => window.getComputedStyle(el).gridTemplateColumns,
      );
      // Should be 4 columns on desktop
      expect(gridColumns.split(" ").length).toBe(4);
    });

    test("chart row should be 2-column on desktop", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      const chartRow = page.locator(".chart-row");
      const gridColumns = await chartRow.evaluate(
        (el) => window.getComputedStyle(el).gridTemplateColumns,
      );
      expect(gridColumns.split(" ").length).toBe(2);
    });
  });

  test.describe("Tablet layout (768x1024)", () => {
    test.use({ viewport: { width: 768, height: 1024 } });

    test("KPI grid should adjust to 2 columns on tablet", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // KPI should be 2 columns on tablet (via media query max-width: 768px)
      const kpiRow = page.locator(".kpi-row");
      const gridColumns = await kpiRow.evaluate(
        (el) => window.getComputedStyle(el).gridTemplateColumns,
      );
      expect(gridColumns.split(" ").length).toBe(2);
    });

    test("chart row should be 1-column on tablet", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      const chartRow = page.locator(".chart-row");
      const gridColumns = await chartRow.evaluate(
        (el) => window.getComputedStyle(el).gridTemplateColumns,
      );
      expect(gridColumns.split(" ").length).toBe(1);
    });

    test("sidebar should still be visible on tablet", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      await expect(page.locator(".el-aside")).toBeVisible();
    });
  });

  test.describe("Mobile layout (375x812)", () => {
    test.use({ viewport: { width: 375, height: 812 } });

    test("page should render without horizontal overflow on mobile", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // Check that body doesn't overflow horizontally
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      const viewportWidth = await page.evaluate(() => window.innerWidth);
      // Allow some tolerance
      expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 50);
    });

    test("KPI cards should stack properly on mobile", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // KPI should be 2 columns on mobile
      const kpiRow = page.locator(".kpi-row");
      const gridColumns = await kpiRow.evaluate(
        (el) => window.getComputedStyle(el).gridTemplateColumns,
      );
      expect(gridColumns.split(" ").length).toBe(2);
    });

    test("dashboard page should be functional on mobile", async ({ page }) => {
      await loginAsStudent(page);
      await page.goto("/dashboard");

      // Page title should be visible
      await expect(page.locator(".page-title")).toBeVisible();

      // KPI cards should be present
      const kpiCards = page.locator(".kpi-card");
      await expect(kpiCards).toHaveCount(4);
    });
  });

  test.describe("Login page responsive", () => {
    test("login card should be centered on desktop", async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 800 });
      await page.goto("/login");
      await page.evaluate(() => localStorage.clear());
      await page.goto("/login");

      const loginCard = page.locator(".login-card");
      await expect(loginCard).toBeVisible();

      // Container should be flex, centered
      const container = page.locator(".login-container");
      const display = await container.evaluate((el) => window.getComputedStyle(el).display);
      expect(display).toBe("flex");
    });

    test("login card should be usable on mobile", async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 812 });
      await page.goto("/login");
      await page.evaluate(() => localStorage.clear());
      await page.goto("/login");

      const loginCard = page.locator(".login-card");
      await expect(loginCard).toBeVisible();

      // Inputs should be usable
      const usernameInput = page.locator('input[placeholder="请输入用户名"]');
      await expect(usernameInput).toBeVisible();
      await usernameInput.fill("test");
      expect(await usernameInput.inputValue()).toBe("test");
    });
  });
});
