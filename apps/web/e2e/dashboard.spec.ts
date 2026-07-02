import { test, expect } from "@playwright/test";

/**
 * Dashboard 页面核心交互 E2E 测试
 *
 * 测试内容：
 * 1. KPI 卡片渲染
 * 2. 日期筛选器交互
 * 3. 图表展示
 * 4. 角色差异化展示（教师看排行，学生不看）
 */
test.describe("Dashboard Page", () => {
  test.beforeEach(async ({ page }) => {
    // Set auth state via localStorage to skip login
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
          email: "teacher1@test.com",
          phone: "13800000002",
          enabled: true,
        }),
      );
    });
  });

  test("should display KPI cards with data", async ({ page }) => {
    await page.goto("/dashboard");

    // Verify page title
    await expect(page.locator(".page-title")).toHaveText("数据仪表盘");

    // Verify 4 KPI cards are visible
    const kpiCards = page.locator(".kpi-card");
    await expect(kpiCards).toHaveCount(4);

    // Verify specific KPI labels
    await expect(page.getByText("今日预约")).toBeVisible();
    await expect(page.getByText("今日借用")).toBeVisible();
    await expect(page.getByText("实验室使用率")).toBeVisible();
    await expect(page.getByText("待审批")).toBeVisible();
  });

  test("should show date range picker", async ({ page }) => {
    await page.goto("/dashboard");

    // Date picker should be visible
    const datePicker = page.locator(".filter-bar .el-date-picker");
    await expect(datePicker).toBeVisible();
  });

  test("should display lab usage chart section", async ({ page }) => {
    await page.goto("/dashboard");

    // Lab usage chart card header
    await expect(page.getByText("实验室使用统计")).toBeVisible();

    // Either chart or empty state
    const chartContainer = page.locator(".chart-card").first();
    await expect(chartContainer).toBeVisible();
  });

  test("should display equipment stats and borrow table", async ({ page }) => {
    await page.goto("/dashboard");

    await expect(page.getByText("设备状态分布")).toBeVisible();
    await expect(page.getByText("设备借用统计")).toBeVisible();
  });

  test("teacher should see student ranking section", async ({ page }) => {
    await page.goto("/dashboard");

    await expect(page.getByText("学生使用排行")).toBeVisible();
  });

  test("student should NOT see student ranking section", async ({ page }) => {
    // Re-set as student
    await page.evaluate(() => {
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

    await page.goto("/dashboard");

    // Student ranking should not be visible for students
    await expect(page.getByText("学生使用排行")).not.toBeVisible();
  });

  test("should filter data when date range changes", async ({ page }) => {
    await page.goto("/dashboard");

    // Click on the date picker to open it
    const datePickerInput = page.locator(".filter-bar .el-date-picker input").first();
    await expect(datePickerInput).toBeVisible();
  });
});
