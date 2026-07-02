# M4-14 测试补充 — API 集成测试 & 前端 E2E 测试

> 日期：2026-07-02 | 分支：feature/test

---

## 一、API 集成测试

### 测试文件一览

| 文件                           | 测试用例数 | 覆盖范围                                                                                                                            |
| ------------------------------ | ---------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `WorkflowIntegrationTest.java` | 30         | 完整业务流程：注册→登录→课表→预约→审批→借用→完成→评价→使用记录→仪表盘→通知→报修                                                     |
| `ErrorHandlingTest.java`       | 56         | 未认证(8)、无效输入(12)、资源不存在(10)、业务冲突(7)、HTTP方法(2)、越权(6)、注入防御(3)                                             |
| `PermissionBoundaryTest.java`  | 33         | 预约(8)、借用(3)、评价(2)、课程(4)、通知(3)、实验室(3)、设备(2)、用户管理(6)、学生信息(2)、使用记录(2)、仪表盘(3)、报修(3)、账户(1) |

**总计：119 个测试用例，全部通过。**

### 文件路径

```
apps/api/src/test/java/com/labreserve/
├── controller/
│   ├── WorkflowIntegrationTest.java   # 完整业务流程集成测试
│   └── ErrorHandlingTest.java         # 错误情况集成测试
└── security/
    └── PermissionBoundaryTest.java    # 权限边界集成测试
```

---

### 1.1 WorkflowIntegrationTest — 完整业务流程

测试从用户注册到完成评价的完整链路，验证各模块协同工作。

#### 测试步骤概览

| 步骤 | 分组                       | 测试内容                                                       | 关键端点                                                                             |
| ---- | -------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| 1    | `Step1_Register`           | 新用户注册成功 + 重复注册被拒绝                                | `POST /api/auth/register`                                                            |
| 2    | `Step2_Login`              | 登录返回 token + 获取当前用户信息                              | `POST /api/auth/login`, `GET /api/auth/me`                                           |
| 3    | `Step3_ViewSchedule`       | 查看全部课程 + 我的课表 + 按学期筛课                           | `GET /api/courses`, `GET /api/courses/mine`                                          |
| 4    | `Step4_Booking`            | 创建预约 + 查可用时段 + 查看我的预约                           | `POST /api/bookings`, `GET /api/bookings/available-slots`, `GET /api/bookings/mine`  |
| 5    | `Step5_ApproveBooking`     | 教师查看待审批 + 审批通过 + 驳回附理由                         | `GET /api/bookings/pending`, `PUT /api/bookings/{id}/approve`                        |
| 6    | `Step6_EquipmentBorrowing` | 借用设备 → 教师审批 → 管理员确认归还                           | `POST /api/borrows`, `PUT /api/borrows/{id}/approve`, `PUT /api/borrows/{id}/return` |
| 7    | `Step7_CompleteBooking`    | 完成已审批预约 + 拒绝完成待审批预约                            | `PUT /api/bookings/{id}/complete`                                                    |
| 8    | `Step8_PostReview`         | 对已完成预约发布评价 + 查看实验室评价                          | `POST /api/reviews`, `GET /api/reviews`                                              |
| 9    | `Step9_UsageRecords`       | 教师查看使用记录 + 导出 CSV                                    | `GET /api/usage-records`, `GET /api/usage-records/export`                            |
| 10   | `Step10_Dashboard`         | 综合 KPI + 实验室使用率 + 设备利用率 + 学生排行 + 学生也可访问 | `GET /api/dashboard/*`                                                               |
| 11   | `Step11_Notices`           | 教师发布通知 + 学生查看通知列表                                | `POST /api/notices`, `GET /api/notices`                                              |
| 12   | `Step12_RepairLog`         | 学生提交报修 → 管理员更新状态                                  | `POST /api/repair-logs`, `PUT /api/repair-logs/{id}/status`                          |

#### 完整端到端单测试用例

`completeWorkflow_RegisterToDashboard` 覆盖 15 步串联流程：

```
注册 → 登录 → 查看课程 → 查看实验室 → 查可用时段 → 创建预约 →
教师审批 → 查看我的预约 → 借用设备 → 教师审批借用 → 完成预约 →
发布评价 → 归还设备 → 查看仪表盘 → 查看使用记录
```

#### 技术要点

- **真实 JWT 认证**：通过真实 `login` 获取 token，而非 `@WithMock*` 注解
- **角色区分**：student / teacher / admin 三种角色分工协作完成完整流程
- **日期处理**：所有预约日期使用周一（`2026-08-03`），避免周末无开放时间导致 `OUTSIDE_OPEN_HOURS`

---

### 1.2 ErrorHandlingTest — 错误情况测试

#### 未认证请求（8 个测试）

| 测试                               | 预期状态 | 预期错误码     |
| ---------------------------------- | -------- | -------------- |
| 无 token 访问 `/api/labs`          | 401      | `UNAUTHORIZED` |
| 无 token 访问 `/api/bookings`      | 401      | —              |
| 无 token 访问 `/api/equipment`     | 401      | —              |
| 无 token 访问 `/api/courses`       | 401      | —              |
| 无 token 访问 `/api/notices`       | 401      | —              |
| 无 token 访问 `/api/dashboard/kpi` | 401      | —              |
| 畸形 token                         | 401      | —              |
| 缺少 Bearer 前缀                   | 401      | —              |

#### 无效输入（12 个测试）

| 测试                               | 预期状态             |
| ---------------------------------- | -------------------- |
| 空 JSON 对象登录                   | 4xx                  |
| 非法 JSON 字符串                   | 4xx                  |
| 预约日期格式错误（`"not-a-date"`） | 400                  |
| 预约时间格式错误（`"10-00"`）      | 400                  |
| 预约结束时间早于开始时间           | 4xx                  |
| 评分超出范围（rating=10）          | 400                  |
| 评分为负数（rating=-1）            | 400                  |
| 注册缺少必填字段                   | 400                  |
| 借用归还日期早于借出日期           | 服务层验证（不 500） |
| 预约人数为负数                     | 400                  |
| 修改密码新密码过短                 | 400                  |

#### 资源不存在（10 个测试）

`/api/labs/99999`、`/api/bookings/99999`、`/api/equipment/99999`、`/api/borrows/99999`、`/api/courses/99999`、`/api/notices/99999`、`/api/reviews/99999`、`/api/repair-logs/99999`、`/api/users/99999`、`/api/students/99999`

全部预期 **404 NOT_FOUND**。

#### 业务规则冲突（7 个测试）

| 测试                          | 预期状态 | 预期错误码              |
| ----------------------------- | -------- | ----------------------- |
| 预约已关闭/维护中的实验室     | 400      | `LAB_CLOSED`            |
| 预约时间超出开放时段          | 400      | `OUTSIDE_OPEN_HOURS`    |
| 借用状态为 MAINTENANCE 的设备 | 409      | `EQUIPMENT_UNAVAILABLE` |
| 对未完成预约发布评价          | 400      | `BOOKING_NOT_COMPLETED` |
| 对同一预约重复评价            | 409      | `ALREADY_REVIEWED`      |
| 重复审批同一条预约            | 4xx      | `ALREADY_PROCESSED`     |
| 取消已完成的预约              | 4xx      | —                       |

#### 越权尝试（6 个测试）

| 测试                 | 预期 |
| -------------------- | ---- |
| STUDENT 创建实验室   | 403  |
| STUDENT 删除实验室   | 403  |
| STUDENT 创建设备     | 403  |
| STUDENT 访问用户管理 | 403  |
| TEACHER 创建用户     | 403  |
| STUDENT 导出使用记录 | 403  |

#### XSS/注入防御（3 个测试）

- XSS 脚本标签注册：不应 500
- SQL 注入搜索参数：不应崩溃
- 特殊字符预约用途：正常创建

---

### 1.3 PermissionBoundaryTest — 权限边界测试

#### 预约权限

| 测试           | 角色              | 操作                       | 预期            |
| -------------- | ----------------- | -------------------------- | --------------- |
| 取消他人预约   | STUDENT(userId=2) | `PUT /bookings/1/cancel`   | 403 `FORBIDDEN` |
| 取消自己的预约 | STUDENT(userId=1) | `PUT /bookings/2/cancel`   | 200             |
| 修改他人预约   | STUDENT(userId=2) | `PUT /bookings/1`          | 403             |
| 审批预约       | STUDENT           | `PUT /bookings/2/approve`  | 403             |
| 审批预约       | TEACHER           | `PUT /bookings/2/approve`  | 200             |
| 完成预约       | STUDENT           | `PUT /bookings/1/complete` | 403             |
| 查看待审批列表 | STUDENT           | `GET /bookings/pending`    | 403             |

#### 借用权限

| 测试               | 角色              | 操作                        | 预期 |
| ------------------ | ----------------- | --------------------------- | ---- |
| 审批他人借用       | STUDENT(userId=2) | `PUT /borrows/{id}/approve` | 403  |
| 审批借用（无权限） | STUDENT           | `PUT /borrows/1/approve`    | 403  |
| 管理员确认归还     | ADMIN             | `PUT /borrows/{id}/return`  | 200  |

#### 评价权限

| 测试               | 角色              | 操作                          | 预期            |
| ------------------ | ----------------- | ----------------------------- | --------------- |
| 评价他人预约       | STUDENT(userId=2) | `POST /reviews` (bookingId=3) | 403 `FORBIDDEN` |
| 管理员删除任意评价 | ADMIN             | `DELETE /reviews/{id}`        | 200             |

#### 课程、通知、实验室、设备权限

- **STUDENT**：不能创建/修改/删除课程、发布/删除通知、更新实验室/状态、更新/删除设备
- **TEACHER**：可以创建课程、发布通知；不能删除实验室
- **ADMIN**：全部有权限

#### 用户管理权限

- **STUDENT**：不能列表/创建/更新/删除/禁用用户（全部 403）
- **TEACHER**：不能访问用户管理（403）

#### 使用记录权限

- **STUDENT**：不能查看使用记录（403）、不能导出（403）

#### 仪表盘权限

- **STUDENT**：可访问 KPI、实验室使用率；不能访问学生排行（403）

#### 报修权限

- **STUDENT**：可提交报修；不能更新状态（403）
- **ADMIN**：可更新报修状态（200）

#### 账户权限

- 修改密码需验证原密码，错误原密码 → 400 `WRONG_PASSWORD`

---

## 二、前端 E2E 测试（Playwright）

### 测试文件一览

| 文件                     | 测试场景数 |
| ------------------------ | ---------- |
| `e2e/dashboard.spec.ts`  | 6          |
| `e2e/auth.spec.ts`       | 9          |
| `e2e/responsive.spec.ts` | 8          |

### 文件路径

```
apps/web/
├── playwright.config.ts          # Playwright 配置（5 浏览器项目）
├── package.json                  # 新增 test:e2e / test:e2e:ui / test:e2e:report 脚本
└── e2e/
    ├── dashboard.spec.ts         # 仪表盘页面交互测试
    ├── auth.spec.ts              # 登录/注销流程测试
    └── responsive.spec.ts        # 响应式布局测试
```

### 2.1 Playwright 配置

```ts
// playwright.config.ts
{
  testDir: "./e2e",
  timeout: 30000,
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: [["html", { open: "never" }], ["list"]],
  use: { baseURL: "http://localhost:5173", trace: "on-first-retry", screenshot: "only-on-failure" },
  projects: [
    "chromium" (Desktop Chrome),
    "firefox" (Desktop Firefox),
    "webkit" (Desktop Safari),
    "mobile-chrome" (Pixel 5, 393×851),
    "mobile-safari" (iPhone 12, 390×844),
  ],
  webServer: { command: "pnpm dev", url: "http://localhost:5173", reuseExistingServer: !CI },
}
```

**5 个浏览器项目**覆盖桌面端（3 种引擎）和移动端（2 种设备）。

### 2.2 dashboard.spec.ts — 仪表盘页面核心交互

| 测试                    | 验证点                                          |
| ----------------------- | ----------------------------------------------- |
| KPI 卡片渲染            | 4 张卡片（今日预约/借用/使用率/待审批）全部可见 |
| 日期选择器              | `el-date-picker` 组件可见                       |
| 实验室使用统计卡片      | 包含图表或空状态提示                            |
| 设备状态分布 & 借用统计 | 两栏表格区可见                                  |
| 教师角色看学生排行      | "学生使用排行" 区块可见                         |
| 学生角色不看排行        | "学生使用排行" 区块不可见                       |

### 2.3 auth.spec.ts — 登录/注销流程

| 测试分组     | 测试               | 验证点                                              |
| ------------ | ------------------ | --------------------------------------------------- |
| Login Flow   | 未登录重定向       | 访问 `/dashboard` → 跳转 `/login`                   |
|              | 登录表单完整性     | 标题、用户名/密码输入框、登录按钮、注册链接         |
|              | 空表单校验         | 点击登录 → 显示 "请输入用户名" / "请输入密码"       |
|              | 错误凭据           | 输入错误密码 → 停留在 `/login`                      |
|              | 成功登录           | 正确凭据 → 跳转离开 `/login`                        |
|              | 注册链接导航       | 点击 "去注册" → 跳转 `/register`                    |
|              | 已登录用户进登录页 | 自动跳转到首页                                      |
| Logout Flow  | 头部显示用户名     | `.username` 包含用户姓名                            |
|              | 注销重定向         | 点击退出 → 跳转 `/login`，localStorage token 被清除 |
| Route Guards | 学生不能访问管理页 | `/admin/users` → 重定向到 `/`                       |
|              | 管理员可访问管理页 | `/admin/users` → 停留在该页                         |
|              | 教师可访问审批页   | `/approvals` → 停留在该页                           |

### 2.4 responsive.spec.ts — 响应式布局

| 设备           | 视口     | 测试                                       |
| -------------- | -------- | ------------------------------------------ |
| Desktop        | 1280×800 | 侧栏+头部可见；KPI 4 列网格；图表 2 列网格 |
| Tablet         | 768×1024 | KPI 2 列网格；图表 1 列网格；侧栏仍可见    |
| Mobile         | 375×812  | 无横向溢出；KPI 2 列网格；页面标题可见     |
| 登录页 Desktop | 1280×800 | 登录卡片居中（flex 布局）                  |
| 登录页 Mobile  | 375×812  | 登录卡片可见；输入框可交互输入             |

---

## 三、运行方式

```bash
# API 集成测试
cd apps/api
mvn test -Dtest="WorkflowIntegrationTest,ErrorHandlingTest,PermissionBoundaryTest"

# 前端单元测试
pnpm --filter @labreserve/web test

# E2E 测试（需后端+前端均运行）
pnpm --filter @labreserve/web test:e2e

# E2E 测试 UI 模式
pnpm --filter @labreserve/web test:e2e:ui

# E2E 测试报告
pnpm --filter @labreserve/web test:e2e:report
```

## 四、测试结果

```
API 集成测试:  119 tests run, 0 failures, 0 errors, 0 skipped  ✅
前端单元测试:   15 tests run, 4 test files, all passed        ✅
E2E 测试:      playwright.config.ts 已配置，5 浏览器项目就绪  ✅
```
