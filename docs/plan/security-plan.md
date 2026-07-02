# 安全问题修复方案

> 基于 docs/security-audit.md | 审计日期：2026-07-02 | 实施状态：✅ 已完成

---

## 一、背景

安全审计发现 16 个安全缺陷（2 Critical + 5 High + 5 Medium + 4 Low）。本方案覆盖 10 项可立即修复的问题，涉及 14 个文件的修改。

## 二、修复清单

### [C-01] JWT 密钥环境变量化

**严重程度**：Critical

**描述**：JWT 签名密钥硬编码在 `application.yml` 中，任何人获取到此密钥即可伪造任意用户的 JWT token。

**文件变更**：

| 文件                                               | 变更                               |
| -------------------------------------------------- | ---------------------------------- |
| `apps/api/src/main/resources/application.yml`      | `jwt.secret` → `${JWT_SECRET:...}` |
| `apps/api/src/test/resources/application-test.yml` | 同上                               |

```yaml
# 修改前
jwt:
  secret: labreserve-jwt-secret-key-change-in-production-2026

# 修改后
jwt:
  secret: ${JWT_SECRET:labreserve-jwt-secret-key-change-in-production-2026}
```

**生产部署要求**：设置环境变量 `JWT_SECRET=<openssl rand -base64 64 生成的随机值>`

---

### [C-02] 数据库密码环境变量化

**严重程度**：Critical

**描述**：MySQL 用户名、密码、root 密码以明文硬编码在 `application.yml` 和 `docker-compose.yml` 中。

**文件变更**：

| 文件                                          | 变更                                                                       |
| --------------------------------------------- | -------------------------------------------------------------------------- |
| `apps/api/src/main/resources/application.yml` | `username`/`password` → `${MYSQL_USERNAME:...}` / `${MYSQL_PASSWORD:...}`  |
| `docker-compose.yml`                          | MySQL/Redis 密码环境变量化，端口绑定 `127.0.0.1`，Redis 添加 `requirepass` |

```yaml
# docker-compose.yml 关键变更
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
  MYSQL_PASSWORD: ${MYSQL_PASSWORD:-labreserve123}

ports:
  - "127.0.0.1:3307:3306" # 仅本地访问

redis:
  command: redis-server --requirepass ${REDIS_PASSWORD:-redis-dev}
  ports:
    - "127.0.0.1:6379:6379"
```

---

### [H-01] 速率限制

**严重程度**：High

**描述**：公开端点无任何速率限制，可被暴力破解和恶意注册。

**文件变更**：

| 文件                                                                     | 操作                           |
| ------------------------------------------------------------------------ | ------------------------------ |
| `apps/api/src/main/java/com/labreserve/config/RateLimitInterceptor.java` | **新增** — IP 级别滑动窗口限流 |
| `apps/api/src/main/java/com/labreserve/config/RateLimitConfig.java`      | **新增** — 注册 Interceptor    |

**限流规则**：

| 路径                     | 限制           | 窗口 |
| ------------------------ | -------------- | ---- |
| `/api/auth/login`        | 10 次/分钟/IP  | 60s  |
| `/api/auth/register`     | 3 次/分钟/IP   | 60s  |
| 其余 `/api/**`           | 120 次/分钟/IP | 60s  |
| `127.0.0.1`（本地/测试） | 无限制         | —    |

**技术实现**：`ConcurrentHashMap<String, WindowCounter>` 内存计数，O(1) 滑动窗口。生产环境可替换为 Redis 计数器。超限返回 HTTP 429 + JSON `{"code":"RATE_LIMITED",...}`。

---

### [H-02] 安全响应头

**严重程度**：High

**描述**：所有 API 响应缺少安全头，易受 MIME 嗅探、Clickjacking 等攻击。

**文件变更**：

| 文件                                                               | 变更                     |
| ------------------------------------------------------------------ | ------------------------ |
| `apps/api/src/main/java/com/labreserve/config/SecurityConfig.java` | 添加 `.headers()` 配置块 |

**添加的响应头**：

| 响应头                      | 值                                    | 作用                     |
| --------------------------- | ------------------------------------- | ------------------------ |
| `X-Content-Type-Options`    | `nosniff`                             | 防 MIME 嗅探             |
| `X-Frame-Options`           | `DENY`                                | 防 Clickjacking          |
| `X-XSS-Protection`          | `1; mode=block`                       | XSS 过滤                 |
| `Cache-Control`             | `no-store`                            | API 响应不缓存           |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HSTS（仅非 dev profile） |

使用 `Environment` 检测 active profile，仅在非 dev 环境启用 HSTS。

---

### [H-03] 全局异常兜底

**严重程度**：High

**描述**：未被捕获的异常（如 NullPointerException）穿透到 Spring Boot 默认错误处理，可能泄露堆栈跟踪。

**文件变更**：

| 文件                                                                        | 变更                          |
| --------------------------------------------------------------------------- | ----------------------------- |
| `apps/api/src/main/java/com/labreserve/handler/GlobalExceptionHandler.java` | 新增 2 个 `@ExceptionHandler` |

**新增处理器**：

```java
// 1. JSON 解析失败 → HTTP 400
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiError> handleMessageNotReadable(...) {
    return 400 + "请求体格式错误，请检查 JSON 格式"
}

// 2. 通用异常兜底 → HTTP 500（记日志，返回通用错误）
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiError> handleGenericException(Exception e) {
    // 放行 Spring 框架异常（405, 415 等）
    if (e instanceof HttpRequestMethodNotSupportedException
            || e instanceof HttpMediaTypeNotSupportedException) {
        throw (RuntimeException) e;
    }
    log.error("Unhandled exception: {}", e.getMessage(), e);
    return 500 + "服务器内部错误"
}
```

---

### [H-04] SQL 日志仅 dev profile 启用

**严重程度**：High

**描述**：`log-impl: StdOutImpl` 将所有 SQL 语句和参数打印到标准输出，生产环境泄露敏感数据。

**文件变更**：

| 文件                                          | 变更                                            |
| --------------------------------------------- | ----------------------------------------------- |
| `apps/api/src/main/resources/application.yml` | 重构为多 profile 结构，SQL 日志移至 dev profile |

**结构变更**：

```yaml
# 公共配置（所有 profile 共享）
spring:
  application:
    name: labreserve-api
  profiles:
    active: dev
server:
  port: 8080
jwt:
  secret: ${JWT_SECRET:...}

---
# dev profile — SQL 日志启用
spring:
  config.activate.on-profile: dev
  datasource: ...
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

---
# prod profile — SQL 日志关闭
spring:
  config.activate.on-profile: prod
  datasource:
    url: ${MYSQL_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  data.redis:
    host: ${REDIS_HOST}
    password: ${REDIS_PASSWORD}
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true # 无 log-impl
```

---

### [H-05] Redis 密码认证

**严重程度**：High

**描述**：Redis 以无密码模式运行，任何能访问 6379 端口的服务都可读写缓存和执行命令。

**文件变更**：

| 文件                                          | 变更                                                    |
| --------------------------------------------- | ------------------------------------------------------- |
| `docker-compose.yml`                          | Redis 添加 `requirepass`，healthcheck 适配              |
| `apps/api/src/main/resources/application.yml` | dev 和 prod profile 都添加 `spring.data.redis.password` |

---

### [M-01] AuthController 补全 @PreAuthorize

**严重程度**：Medium

**描述**：`/me` 和 `/change-password` 缺少方法级 `@PreAuthorize` 注解，依赖全局 URL 规则保护。

**文件变更**：

| 文件                                                                   | 变更                                                                  |
| ---------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `apps/api/src/main/java/com/labreserve/controller/AuthController.java` | `/me` 和 `/change-password` 添加 `@PreAuthorize("isAuthenticated()")` |

---

### [M-02] 密码修改后使旧 token 失效

**严重程度**：Medium

**描述**：用户修改密码后，旧的 JWT token 仍然有效直到自然过期（24h），无法阻止已泄露 token 的继续使用。

**方案**：基于 Redis 的 token 版本号机制

| 文件                           | 变更                                                                                      |
| ------------------------------ | ----------------------------------------------------------------------------------------- |
| `JwtUtil.java`                 | 新增 `generateToken(userId, username, role, tokenVersion)` 重载 + `extractTokenVersion()` |
| `AuthService.java`             | `changePassword()` 中递增 Redis `user:{id}:tokenVersion`，login 时携带当前版本号          |
| `JwtAuthenticationFilter.java` | 校验 token 中 version ≥ Redis 中 version，不匹配则拒绝                                    |

**降级策略**：

- `RedissonClient` 使用 `@Autowired(required = false)` 注入
- Redis 不可用时 `isTokenVersionValid()` 返回 true（允许通过）
- 测试环境（Redis 被 exclude 时）正常工作

**Redis 键设计**：

```
Key:   user:{userId}:tokenVersion
Value: Long (自增版本号)
TTL:   JWT 过期时间 + 5 分钟（超过 JWT 有效期后自动清理）
```

---

### [L-04] 前端 401 改用 router.push

**严重程度**：Low

**描述**：API 客户端收到 401 时使用 `window.location.href = "/login"` 硬重定向，丢失 SPA 状态。

**文件变更**：

| 文件                         | 变更                                                            |
| ---------------------------- | --------------------------------------------------------------- |
| `apps/web/src/api/client.ts` | 新增 `setRouter()` 导出，401 响应中调用 `router.push("/login")` |
| `apps/web/src/main.ts`       | 调用 `setRouter(router)` 注入路由实例                           |

**降级**：如果 router 未注入（如测试环境），回退到 `window.location.href`。

---

## 三、未修复项目

| 问题                    | 原因                                              |
| ----------------------- | ------------------------------------------------- |
| M-03 注册验证码         | 需要第三方服务/邮件服务                           |
| M-04 localStorage token | 改为 httpOnly cookie 需较大架构改动               |
| M-05 Docker 端口暴露    | 开发环境需要，已通过密码加固和 127.0.0.1 绑定缓解 |
| L-01 CSRF 策略          | 纯 Bearer token API 不受 CSRF 影响                |
| L-02 健康检查信息       | 可后续优化，不构成直接威胁                        |
| L-03 注解重构           | 全局 `authenticated()` 规则提供兜底               |

## 四、验证

```
mvn test → 292 tests, 0 failures ✅
pnpm test → 15 tests, 4 files passed ✅
```

## 五、文件清单

```
修改的文件 (14):
├── apps/api/src/main/resources/application.yml
├── apps/api/src/test/resources/application-test.yml
├── docker-compose.yml
├── apps/api/src/main/java/com/labreserve/
│   ├── config/SecurityConfig.java
│   ├── config/JwtUtil.java
│   ├── config/JwtAuthenticationFilter.java
│   ├── config/RateLimitInterceptor.java          [新增]
│   ├── config/RateLimitConfig.java               [新增]
│   ├── handler/GlobalExceptionHandler.java
│   ├── service/AuthService.java
│   └── controller/AuthController.java
├── apps/web/src/api/client.ts
└── apps/web/src/main.ts

新增的文件 (2):
├── docs/security-audit.md
└── docs/plan/security-plan.md  (本文件)
```
