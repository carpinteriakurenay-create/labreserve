# v1.0.0 首次发布

## ✅ 测试结果

| 测试类型 | 工具 | 数量 | 结果 |
|---------|------|------|------|
| 后端集成测试 | JUnit 5 + H2 | 293 | BUILD SUCCESS |
| 前端单元测试 | Vitest + Vue Test Utils | 15 | all passed |
| 前端构建 | Vite | — | built in 21s |
| E2E 测试 | Playwright (5 browsers) | 23 specs | ✅ |

## 📋 变更摘要

LabReserve 高校实验室预约管理系统的首次正式发布，包含完整的后端 API、前端 SPA、安全加固和性能优化。

### 核心功能

- **认证系统**：JWT 无状态认证 + STUDENT/TEACHER/ADMIN 三级角色
- **核心业务**：实验室管理、预约管理（提交→审批→完成）、设备借用、课程管理
- **数据统计**：Dashboard KPI + 使用率图表 + 学生排行
- **辅助功能**：通知公告、评价、使用记录、报修、学生信息
- **67 个 REST API 端点**，覆盖 14 类资源

### 安全加固 (10 项)

| 编号 | 修复 | 状态 |
|------|------|------|
| C-01 | JWT 密钥环境变量化 | ✅ |
| C-02 | 数据库密码环境变量化 + Docker Compose | ✅ |
| H-01 | 速率限制（登录 10/min、注册 3/min） | ✅ |
| H-02 | 安全响应头（X-Content-Type-Options, X-Frame-Options, HSTS） | ✅ |
| H-03 | 全局异常兜底处理 | ✅ |
| H-04 | SQL 日志仅 dev profile + prod 配置分离 | ✅ |
| H-05 | Docker Compose Redis 密码 + 127.0.0.1 绑定 | ✅ |
| M-01 | AuthController 补全 @PreAuthorize 注解 | ✅ |
| M-02 | 密码修改后使旧 JWT token 失效（Redis token 版本号） | ✅ |
| L-04 | 前端 401 改用 `router.push` | ✅ |

### 性能优化 (8 项)

| 编号 | 优化 | 状态 |
|------|------|------|
| DB-01 | 测试 schema 30 个数据库索引 | ✅ |
| DB-02 | Dashboard getKpi SQL COUNT(DISTINCT) | ✅ |
| DB-03 | Dashboard 聚合下推到 SQL | ✅ |
| DB-04 | CSV 导出 LIMIT 10000 | ✅ |
| FE-01 | Vite 代码分割 (vendor-vue/element/echarts) | ✅ |
| FE-02 | ECharts 按需导入 (bundle -85%) | ✅ |
| FE-03 | MyBookings N+1 → Promise.allSettled 并行 | ✅ |
| FE-04 | Dashboard 三端点 @Cacheable | ✅ |

### 文档 (7 项新增)

- `README.md`、`CHANGELOG.md`、`docs/deployment.md`
- `docs/security-audit.md`、`docs/plan/security-plan.md`
- `docs/performance-audit.md`、`docs/plan/performance-plan.md`
- `docs/architecture.md` 新增"已知技术债和改进方向"

### 📊 变更统计

```
61 files changed, 7009 insertions(+), 221 deletions(-)
  39 modified, 20 new files
  后端: 27 files (安全/性能/测试/限流/聚合查询)
  前端: 10 files (bundle/401修复/TSDoc/E2E/config)
  Shared: 8 files (TSDoc注释)
  Docs: 12 files
  其他: 4 files (Docker, Config, Root)
```
