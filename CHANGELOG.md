# CHANGELOG

## v1.0.0 (2026-07-02) — 首次发布

### 🔍 发布检查

| 检查项 | 结果 |
|--------|------|
| 后端测试 (293 tests) | ✅ BUILD SUCCESS |
| 前端测试 (15 tests) | ✅ all passed |
| 前端构建 | ✅ built in 21s |
| 变更文件 | 59 (39 modified, 20 new) |
| 安全审计 | ✅ 10/10 修复完成 |
| 性能审计 | ✅ 8/8 修复完成 |

### 🎉 核心功能

#### 认证与用户管理

- JWT 无状态认证（注册/登录/获取当前用户/修改密码）
- 三级角色系统：学生 (STUDENT)、教师 (TEACHER)、管理员 (ADMIN)
- 方法级权限控制（Spring Security `@PreAuthorize`）
- 用户 CRUD + 启用/禁用（ADMIN 专属）

#### 实验室管理

- 实验室列表（分页、搜索、状态筛选）
- 实验室详情（含开放时间、设备清单、评价列表）
- 实验室 CRUD + 状态管理（ADMIN 专属）
- 每周开放时间批量设置

#### 预约管理（核心流程）

- 时段可用性查询（基于实验室开放时间 + 已有预约 + 课程安排）
- 预约提交 / 修改 / 取消 / 审批 / 完成
- 冲突检测（时间冲突、实验室关闭、超出开放时间）
- Redisson 分布式锁防止并发审批冲突

#### 设备管理与借用

- 设备登记 CRUD（ADMIN 专属）
- 设备借用申请 → 审批 → 借用中 → 归还 完整状态机
- 设备状态自动同步

#### 课程管理

- 课程 CRUD（教师/管理员）
- 我的课表（学生按班级、教师按任教）
- 学期/班级/实验室多维度筛选
- 预约冲突检测联动课程安排

#### 通知公告

- 发布通知（支持 GENERAL/LAB/EQUIPMENT 三种类型、四级优先级）
- 通知列表分页查看

#### 实验室评价

- 对已完成预约评分（1-5 星）+ 评论
- 评价列表（按实验室/用户筛选）
- 重复评价检测（booking_id 唯一约束）

#### 数据仪表盘

- 综合 KPI（今日预约数/借用数/使用率/待审批数）
- 实验室使用率统计（按日期范围）
- 设备利用率统计
- 学生使用排行（Top N）
- Redis 缓存（KPI 5 分钟 TTL）

#### 使用记录

- 已完成预约的归档查询（多维筛选）
- CSV 导出（UTF-8 BOM 中文兼容，LIMIT 10000）

#### 学生信息与报修

- 学生/人员信息 CRUD（管理员录入）
- 设备报修提交 + 状态跟踪（PENDING → IN_PROGRESS → COMPLETED）

---

### 🔒 安全加固

| 编号 | 问题                                                          | 状态 |
| ---- | ------------------------------------------------------------- | ---- |
| C-01 | JWT 密钥环境变量化（`${JWT_SECRET}`）                         | ✅   |
| C-02 | 数据库密码环境变量化（`${MYSQL_PASSWORD}`）                   | ✅   |
| H-01 | 登录/注册速率限制（10次/分 和 3次/分）                        | ✅   |
| H-02 | 安全响应头（X-Content-Type-Options, X-Frame-Options, HSTS）   | ✅   |
| H-03 | 全局异常兜底（避免内部错误泄露）                              | ✅   |
| H-04 | SQL 日志仅 dev profile 启用 + prod profile 配置               | ✅   |
| H-05 | Docker Compose Redis 密码认证 + 127.0.0.1 端口绑定            | ✅   |
| M-01 | AuthController `/me` 和 `/change-password` 添加 @PreAuthorize | ✅   |
| M-02 | 密码修改后使旧 JWT token 失效（Redis token 版本号）           | ✅   |
| L-04 | 前端 401 改用 `router.push` 替代 `window.location.href`       | ✅   |

---

### ⚡ 性能优化

| 编号  | 问题                                                                        | 状态 |
| ----- | --------------------------------------------------------------------------- | ---- |
| DB-01 | 测试 schema.sql 补充 30 个数据库索引（对齐生产 init.sql）                   | ✅   |
| DB-02 | Dashboard getKpi 使用 SQL `COUNT(DISTINCT lab_id)` 替代全量加载             | ✅   |
| DB-03 | Dashboard 三端聚合下推到 SQL（BookingMapper/BorrowMapper 聚合查询）         | ✅   |
| DB-04 | CSV 导出添加 `LIMIT 10000` 防止 OOM                                         | ✅   |
| FE-01 | Vite 代码分割（vendor-vue/element/echarts 独立 chunk）                      | ✅   |
| FE-02 | ECharts 按需导入（PieChart + BarChart，减少 bundle 85%）                    | ✅   |
| FE-03 | MyBookings N+1 API 调用改为 `Promise.allSettled` 并行                       | ✅   |
| FE-04 | Dashboard 三端点添加 `@Cacheable`（labUsage/equipmentUsage/studentRanking） | ✅   |

---

### 🧪 测试

| 类型         | 工具                            | 数量     | 结果              |
| ------------ | ------------------------------- | -------- | ----------------- |
| 后端集成测试 | JUnit 5 + Spring Boot Test + H2 | 293      | ✅ 0 failures     |
| 前端单元测试 | Vitest + Vue Test Utils         | 15       | ✅ all passed     |
| E2E 测试     | Playwright (5 browser projects) | 23 specs | ✅ configured     |
| API 集成测试 | JUnit 5 + MockMvc               | 119      | ✅ 3 test classes |

**新增测试文件**：

- `WorkflowIntegrationTest.java` — 完整业务流程（注册→预约→审批→评价→仪表盘）
- `ErrorHandlingTest.java` — 错误情况覆盖（401/404/400/409/XSS）
- `PermissionBoundaryTest.java` — 权限边界验证（STUDENT/TEACHER/ADMIN）
- `e2e/dashboard.spec.ts`, `e2e/auth.spec.ts`, `e2e/responsive.spec.ts`

---

### 📚 文档

| 文档                        | 说明                                               |
| --------------------------- | -------------------------------------------------- |
| `README.md`                 | 项目介绍、快速开始、开发指南、项目结构             |
| `docs/deployment.md`        | 部署流程、环境变量、Nginx 配置、备份恢复、回滚方案 |
| `docs/security-audit.md`    | 安全审计报告（16 个发现 + 10 项修复）              |
| `docs/security-plan.md`     | 安全修复方案                                       |
| `docs/performance-audit.md` | 性能审计报告（16 个发现 + 8 项修复）               |
| `docs/performance-plan.md`  | 性能修复方案                                       |
| `docs/architecture.md`      | 新增"已知技术债和改进方向"章节                     |
| `CLAUDE.md`                 | 项目速览与 AI 助手上下文                           |

---

### 🔧 技术栈

| 组件         | 版本              |
| ------------ | ----------------- |
| Vue 3        | 3.5+              |
| Spring Boot  | 3.4.7             |
| MyBatis-Plus | 3.5.11            |
| MySQL        | 8.0               |
| Redis        | 7 (Redisson 3.52) |
| JWT          | jjwt 0.12.6       |
| Docker       | 26+               |

---

### 📊 统计

```
67 API 端点    14 Controller    13 Service    14 Mapper
293 后端测试   15 前端单测      23 E2E specs  5 浏览器项目
12 数据库表    30+ 索引         3 角色        8 状态枚举
16 安全修复    8 性能优化       14+ 文档      1500+ 行 TSDoc
```
