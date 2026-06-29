# CLAUDE.md

## 项目：LabReserve — 高校实验室预约管理系统

### 技术栈

- **前端**：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router
- **后端**：Java 21 + Spring Boot 3.4 + MyBatis-Plus 3.5 + MySQL 8.0
- **缓存**：Redis 7
- **测试**：Vitest（前端单元测试）+ JUnit 5（后端单元测试）+ Postman / Apifox（接口测试）
- **部署**：Docker + Nginx
- **包管理**：pnpm workspace（前端）+ Maven（后端）

### 项目结构

```
E:/ULRS/
├── apps/
│   ├── web/                    # Vue 3 前端应用（@labreserve/web）
│   │   ├── src/
│   │   │   ├── api/            # API 请求封装
│   │   │   ├── assets/         # 静态资源
│   │   │   ├── components/     # 公共组件
│   │   │   ├── composables/    # 组合式 API / Hooks
│   │   │   ├── router/         # 路由配置
│   │   │   ├── stores/         # Pinia 状态管理
│   │   │   ├── styles/         # 全局样式
│   │   │   ├── views/          # 页面组件
│   │   │   ├── App.vue
│   │   │   └── main.ts
│   │   ├── vite.config.ts
│   │   └── vitest.config.ts
│   └── api/                    # Spring Boot 后端服务
│       ├── src/main/java/com/labreserve/
│       │   ├── controller/     # REST 控制器
│       │   ├── service/        # 业务逻辑层
│       │   ├── mapper/         # MyBatis-Plus Mapper
│       │   ├── entity/         # 数据库实体
│       │   └── config/         # Spring 配置
│       └── src/main/resources/
│           └── application.yml
├── packages/
│   ├── shared/                 # 前后端共享类型和工具函数（@labreserve/shared）
│   │   └── src/
│   │       ├── index.ts        # 枚举、接口定义
│   │       └── utils.ts        # 通用工具函数
│   └── tsconfig/               # 共享 TypeScript 配置（@labreserve/tsconfig）
│       ├── base.json           # 基础配置（ES2022, strict）
│       └── vue.json            # Vue 项目配置（extends base）
├── docker-compose.yml          # 本地开发环境（MySQL 8.0 + Redis 7）
├── docs/                       # 设计文档、ADR、接口定义
├── .github/workflows/ci.yml    # GitHub Actions CI
├── eslint.config.mjs           # ESLint 9 flat config
├── .prettierrc                 # Prettier 配置
├── commitlint.config.mjs       # Conventional Commits 规范
└── .husky/                     # Git hooks（pre-commit + commit-msg）
```

### 开发规范

- **语言**：代码命名和注释使用英文，文档和 Git 提交说明使用中文
- **Git 提交**：遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式
  - `feat:` 新功能
  - `fix:` 修复 Bug
  - `docs:` 文档变更
  - `style:` 代码格式（不影响逻辑）
  - `refactor:` 重构
  - `test:` 测试相关
  - `chore:` 构建 / 工具 / 依赖变更
- **测试**：新增功能必须有对应的测试用例。前端用 Vitest，后端用 JUnit 5
- **代码风格**：遵循项目 ESLint + Prettier 配置，pre-commit hook 会自动修复
- **Vue 组件**：使用 `<script setup lang="ts">` 组合式 API 写法
- **API 路径**：遵循 RESTful 风格（`/api/resource` 命名复数形式）

### 分支策略

- `main`：生产分支，只接受 PR 合并
- `develop`：开发分支，日常开发基于此分支
- `feature/*`：功能分支，从 develop 创建，完成后 PR 回 develop

### 本地开发

```bash
# 前端
pnpm dev:web                        # 启动 Vite 开发服务器 :5173

# 后端（需要 Java 21 + Maven）
cd apps/api && mvn spring-boot:run  # 启动 Spring Boot :8080

# 基础设施
docker compose up -d                 # 启动 MySQL + Redis

# 代码检查
pnpm lint                            # ESLint 检查
pnpm format:check                    # Prettier 格式检查
pnpm typecheck                       # TypeScript 类型检查
pnpm test                            # 运行所有测试
```
