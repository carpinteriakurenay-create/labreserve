# LabReserve — 高校实验室预约管理系统

> 基于 Vue 3 + Spring Boot 3 的现代化实验室管理平台

[![CI](https://github.com/your-org/labreserve/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/labreserve/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.7-green)
![Vue](https://img.shields.io/badge/Vue-3.5+-4FC08D)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 目录

- [项目简介](#项目简介)
- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [测试](#测试)
- [部署](#部署)
- [文档索引](#文档索引)

## 项目简介

LabReserve 是面向高校实验室场景的预约管理系统，支持学生在线预约实验室、借用设备、评价实验室，教师审批管理，管理员全面配置实验室资源。系统采用前后端分离架构，支持容器化部署。

### 核心角色

| 角色               | 权限                                                                   |
| ------------------ | ---------------------------------------------------------------------- |
| **学生 (STUDENT)** | 注册登录、浏览实验室、预约/取消预约、借用/归还设备、查看课表、发布评价 |
| **教师 (TEACHER)** | 学生全部权限 + 审批预约/借用、发布通知、查看使用记录与统计             |
| **管理员 (ADMIN)** | 全部权限：用户管理、实验室 CRUD、设备管理、课程管理、数据导出          |

### 设计理念

- **RESTful API**：67 个接口覆盖 14 类资源，统一响应格式 `{code, message, data}`
- **安全优先**：JWT 认证 + Spring Security 方法级授权 + 速率限制 + 安全响应头
- **性能优化**：Redis 缓存 + SQL 聚合查询 + 代码分割 + ECharts 按需加载
- **容器化部署**：Docker Compose 本地开发 + 生产级 Dockerfile + Nginx 反向代理

## 功能概览

| 模块       | 功能                                         | 状态 |
| ---------- | -------------------------------------------- | ---- |
| 认证       | 注册 / 登录 / 修改密码 / 获取当前用户        | ✅   |
| 实验室管理 | 列表 / 详情 / CRUD / 状态管理 / 开放时间设置 | ✅   |
| 预约管理   | 时段查询 / 提交预约 / 审批 / 取消 / 完成     | ✅   |
| 设备管理   | 列表 / 详情 / CRUD / 状态管理                | ✅   |
| 设备借用   | 申请 / 审批 / 归还确认                       | ✅   |
| 课程管理   | CRUD / 我的课表 / 班级筛选                   | ✅   |
| 通知公告   | 发布 / 列表 / 详情 / 删除                    | ✅   |
| 实验室评价 | 提交评价 / 查看评价 / 删除                   | ✅   |
| 使用记录   | 查询 / CSV 导出                              | ✅   |
| 数据仪表盘 | KPI / 实验室使用率 / 设备利用率 / 学生排行   | ✅   |
| 学生信息   | CRUD                                         | ✅   |
| 报修记录   | 提交 / 状态更新                              | ✅   |

## 技术栈

| 层次            | 技术                    | 版本                  |
| --------------- | ----------------------- | --------------------- |
| **前端框架**    | Vue 3 + TypeScript      | 3.5+ / 5.8            |
| **构建工具**    | Vite                    | 6.x                   |
| **UI 组件库**   | Element Plus            | 2.x                   |
| **状态管理**    | Pinia                   | 3.x                   |
| **路由**        | Vue Router              | 4.x                   |
| **图表**        | ECharts                 | 6.x                   |
| **HTTP 客户端** | Axios                   | 1.x                   |
| **后端框架**    | Spring Boot             | 3.4.7                 |
| **安全框架**    | Spring Security + JWT   | 6.x / jjwt 0.12       |
| **ORM**         | MyBatis-Plus            | 3.5.11                |
| **数据库**      | MySQL                   | 8.0                   |
| **缓存**        | Redis                   | 7 (via Redisson 3.52) |
| **容器**        | Docker + Docker Compose | 26+                   |
| **CI/CD**       | GitHub Actions          | —                     |

## 快速开始

### 前置要求

- **Java 17+** (推荐 Temurin 发行版)
- **Maven 3.9+**
- **Node.js 20+** + **pnpm 9+**
- **Docker Desktop** (可选，用于本地 MySQL/Redis)

### 1. 克隆项目

```bash
git clone https://github.com/your-org/labreserve.git
cd labreserve
```

### 2. 启动基础设施

```bash
docker compose up -d
```

这将启动：

- MySQL 8.0 → `localhost:3307` (数据库 `labreserve_dev`)
- Redis 7 → `localhost:6379`

### 3. 启动后端

```bash
cd apps/api
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`，健康检查：`GET /api/health`

### 4. 启动前端

```bash
pnpm install        # 首次运行需要安装依赖
pnpm dev:web        # 或 pnpm --filter @labreserve/web dev
```

前端运行在 `http://localhost:5173`，API 请求自动代理到 `localhost:8080`。

### 5. 初始账号

| 用户名    | 密码          | 角色   |
| --------- | ------------- | ------ |
| `admin`   | `admin123`    | 管理员 |
| `T001`    | `password123` | 教师   |
| `2021001` | `password123` | 学生   |

> 以上账号通过 `docker/mysql/init.sql` 种子数据创建（Docker 环境）。本地 MySQL 环境需手动创建用户或通过注册接口注册。

## 项目结构

```
labreserve/
├── apps/
│   ├── web/                          # Vue 3 前端应用
│   │   ├── src/
│   │   │   ├── api/                  # API 请求封装 (按资源模块)
│   │   │   ├── components/           # 公共组件
│   │   │   ├── composables/          # 组合式 API / Hooks
│   │   │   ├── router/               # 路由配置（懒加载）
│   │   │   ├── stores/               # Pinia 状态管理
│   │   │   ├── styles/               # 全局样式
│   │   │   ├── views/                # 页面组件 (20 个视图)
│   │   │   └── __tests__/            # 前端单元测试
│   │   ├── e2e/                      # Playwright E2E 测试
│   │   ├── vite.config.ts
│   │   ├── playwright.config.ts
│   │   └── vitest.config.ts
│   └── api/                          # Spring Boot 后端服务
│       ├── src/main/java/com/labreserve/
│       │   ├── controller/           # REST 控制器 (14 个)
│       │   ├── service/              # 业务逻辑层 (13 个)
│       │   ├── mapper/               # MyBatis-Plus Mapper (14 个)
│       │   ├── entity/               # 数据库实体 (13 个)
│       │   ├── dto/                  # 请求/响应 DTO
│       │   ├── enums/                # 枚举定义
│       │   ├── config/               # Spring 配置（安全、JWT、缓存、限流）
│       │   ├── handler/              # 全局异常处理
│       │   └── exception/            # 业务异常
│       └── src/test/                 # 后端测试 (30 个测试类)
├── packages/
│   ├── shared/                       # 前后端共享类型和工具
│   │   └── src/
│   │       ├── index.ts              # 类型导出入口
│   │       ├── types/                # API 类型、枚举、请求/响应接口
│   │       └── utils.ts              # 工具函数
│   └── tsconfig/                     # 共享 TypeScript 配置
├── docker/
│   └── mysql/                        # MySQL 初始化脚本
│       └── init.sql                  # 建表 + 种子数据
├── docs/                             # 项目文档
│   ├── api-design.md                 # API 设计文档 (67 个接口)
│   ├── architecture.md              # 架构设计文档 + ADR
│   ├── deployment.md                 # 部署指南
│   ├── security-audit.md            # 安全审计报告
│   ├── performance-audit.md         # 性能审计报告
│   ├── data-model.md                 # 数据模型
│   ├── bug/                          # Bug 修复记录
│   └── plan/                         # 修复方案文档
├── docker-compose.yml                # 本地开发基础设施
├── pnpm-workspace.yaml               # pnpm monorepo 配置
└── CLAUDE.md                         # Claude 项目指引
```

## 开发指南

### 代码规范

- **Git 提交**：遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式
  - `feat:` 新功能 | `fix:` 修复 | `docs:` 文档 | `refactor:` 重构 | `test:` 测试 | `chore:` 构建/工具
- **代码注释**：代码、变量、函数使用英文；Git 提交和文档使用中文
- **Vue 组件**：使用 `<script setup lang="ts">` 组合式 API
- **API 路径**：RESTful 风格，`/api/{resource}` 名词复数

### 开发工作流

```bash
# 1. 从 develop 创建功能分支
git checkout develop
git pull
git checkout -b feature/my-feature

# 2. 开发 + 测试
pnpm dev:web                                    # 启动前端 dev server
cd apps/api && mvn spring-boot:run              # 启动后端
pnpm test                                        # 运行所有测试
cd apps/api && mvn test                          # 后端测试

# 3. 代码检查
pnpm lint                                        # ESLint
pnpm format:check                                # Prettier
pnpm typecheck                                   # TypeScript

# 4. 提交
git add .
git commit -m "feat: 添加新功能"
git push origin feature/my-feature

# 5. 创建 PR 到 develop
```

### 添加新功能

1. **后端**：entity → mapper → service → controller → DTO → 测试
2. **前端**：shared types → api module → view → router → 测试
3. 遵循 TDD：先写测试，再写实现

### 调试

- **后端日志**：dev profile 默认输出 SQL 日志。使用 `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"` 开启远程调试。
- **前端调试**：Vite dev server 支持 HMR，浏览器 Vue DevTools 可用。
- **数据库**：直接连接 `localhost:3307` (Docker) 或 `localhost:3306` (本地 MySQL)，用户名 `labreserve`，密码 `labreserve123`。

## 测试

### 测试矩阵

| 类型         | 工具                    | 命令            | 覆盖范围                           |
| ------------ | ----------------------- | --------------- | ---------------------------------- |
| 前端单元测试 | Vitest + Vue Test Utils | `pnpm test`     | 组件、Store、路由守卫、API 客户端  |
| 后端单元测试 | JUnit 5 + Mockito       | `mvn test`      | Service 逻辑、工具类               |
| 后端集成测试 | Spring Boot Test + H2   | `mvn test`      | Controller、安全边界、完整业务流程 |
| E2E 测试     | Playwright              | `pnpm test:e2e` | 登录流程、仪表盘交互、响应式布局   |

### 测试统计

```
后端: 293 tests, 0 failures
前端: 15 unit tests
E2E:  23 spec cases across 3 suites (5 browser projects)
```

## 部署

详细部署指南请参阅 [docs/deployment.md](docs/deployment.md)。

### Docker 部署（快速体验）

```bash
# 构建镜像
docker build -t labreserve-api -f apps/api/Dockerfile .
docker build -t labreserve-web -f apps/web/Dockerfile .

# 启动
docker compose -f docker-compose.prod.yml up -d
```

### 环境变量

| 变量             | 必填    | 说明           | 默认值       |
| ---------------- | ------- | -------------- | ------------ |
| `JWT_SECRET`     | ✅ 生产 | JWT 签名密钥   | —            |
| `MYSQL_URL`      | ✅ 生产 | 数据库连接 URL | —            |
| `MYSQL_USERNAME` | ✅ 生产 | 数据库用户名   | `labreserve` |
| `MYSQL_PASSWORD` | ✅ 生产 | 数据库密码     | —            |
| `REDIS_HOST`     | ✅ 生产 | Redis 地址     | —            |
| `REDIS_PASSWORD` | ✅ 生产 | Redis 密码     | —            |

## 文档索引

| 文档                                                           | 说明                      |
| -------------------------------------------------------------- | ------------------------- |
| [CLAUDE.md](CLAUDE.md)                                         | 项目速览与快速参考        |
| [docs/api-design.md](docs/api-design.md)                       | API 接口设计（67 个接口） |
| [docs/architecture.md](docs/architecture.md)                   | 架构设计 + 架构决策记录   |
| [docs/deployment.md](docs/deployment.md)                       | 部署流程与运维指南        |
| [docs/data-model.md](docs/data-model.md)                       | 数据库设计                |
| [docs/security-audit.md](docs/security-audit.md)               | 安全审计报告              |
| [docs/performance-audit.md](docs/performance-audit.md)         | 性能审计报告              |
| [docs/plan/security-plan.md](docs/plan/security-plan.md)       | 安全修复方案              |
| [docs/plan/performance-plan.md](docs/plan/performance-plan.md) | 性能修复方案              |

## License

MIT License © 2026 LabReserve
