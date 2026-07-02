## v1.0.0 首次发布

### 🔍 检查清单

| 检查项               | 结果                     |
| -------------------- | ------------------------ |
| 后端测试 (293 tests) | ✅ BUILD SUCCESS         |
| 前端测试 (15 tests)  | ✅ all passed            |
| 前端构建             | ✅ built in 21s          |
| 变更文件             | 61 (45 modified, 16 new) |
| 安全审计             | ✅ 10/10 修复完成        |
| 性能审计             | ✅ 8/8 修复完成          |

### 📋 变更摘要

LabReserve 高校实验室预约管理系统的首次正式发布，包含完整的后端 API、前端 SPA、安全加固和性能优化。

**核心功能**：

- 认证系统：JWT 无状态认证 + STUDENT/TEACHER/ADMIN 三级角色
- 核心业务：实验室管理、预约管理（提交→审批→完成）、设备借用、课程管理
- 数据统计：Dashboard KPI + 使用率图表 + 学生排行
- 辅助功能：通知公告、评价、使用记录、报修、学生信息
- **67 个 REST API 端点**，覆盖 14 类资源

**安全加固** (10 项)：

- JWT 密钥 + 数据库密码环境变量化
- 速率限制（登录 10/min、注册 3/min）
- 安全响应头 + HSTS + 全局异常兜底
- token 版本号机制（密码修改后旧 token 失效）
- Docker Compose Redis 密码 + 127.0.0.1 绑定
- SQL 日志 dev-only + prod 配置分离

**性能优化** (8 项)：

- 测试 schema 30 个数据库索引
- Dashboard SQL 聚合下推
- CSV 导出 LIMIT 10000
- Vite 代码分割 (vendor-vue/element/echarts)
- ECharts 按需导入 (bundle -85%)
- MyBookings N+1 → Promise.allSettled 并行
- Dashboard 三端点 @Cacheable

**文档** (8 项新增)：

- README.md、CHANGELOG.md、docs/deployment.md
- docs/security-audit.md、docs/security-plan.md
- docs/performance-audit.md、docs/performance-plan.md
- docs/architecture.md 新增"已知技术债和改进方向"

### 📊 变更统计

```
61 files changed, 6979 insertions(+), 221 deletions(-)
  45 modified, 16 new files
  后端: 27 files (安全/性能/测试/限流/聚合查询)
  前端: 10 files (bundle/401修复/TSDoc/E2E/config)
  Shared: 8 files (TSDoc注释)
  Docker/Config: 2 files
  Docs: 10 files
  Root: 4 files (README, CHANGELOG, pnpm-lock)
```
