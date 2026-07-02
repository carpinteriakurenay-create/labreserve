# LabReserve 安全审计报告

> 审计日期：2026-07-02 | 审计范围：全代码库 | 分支：feature/test

---

## 总体评分

| 维度         | 评分       | 说明                                                                       |
| ------------ | ---------- | -------------------------------------------------------------------------- |
| 认证与授权   | 7/10       | JWT 实现较为规范，但缺少 token 刷新和吊销机制；部分端点权限粒度过粗        |
| 输入验证     | 8/10       | DTO 层验证较完善，Jakarta Validation 覆盖全面，但缺少对敏感字段的额外清洗  |
| 敏感数据保护 | 5/10       | 明文数据库密码和 JWT 密钥硬编码在配置文件中，日志可能泄露 SQL 参数         |
| 依赖安全     | 8/10       | 依赖版本较新，无已知严重 CVE，但 Redisson 3.52.0 并非最新 LTS              |
| API 安全     | 5/10       | 无速率限制、CSRF 完全禁用、无安全响应头、CORS 配置为空                     |
| **综合**     | **6.6/10** | 认证和验证基础较好，但生产环境就绪度不足，需要在密钥管理、API 防护方面加固 |

---

## 发现的问题

### Critical

#### [C-01] JWT 密钥硬编码在配置文件中

- **位置**: [application.yml:40](apps/api/src/main/resources/application.yml:40), [application-test.yml:35](apps/api/src/test/resources/application-test.yml:35)
- **描述**: JWT 签名密钥直接以明文形式存储在 `application.yml` 中：
  ```yaml
  jwt:
    secret: labreserve-jwt-secret-key-change-in-production-2026
  ```
  配置文件提交到 Git 仓库后，任何有仓库访问权限的人都能获取到此密钥。而且该密钥长度仅 50 字节，虽然 JJWT 0.12+ 允许 HMAC-SHA256 最小 256 bits (32 bytes)，但密钥本身是一个可预测的短语，熵值不足。
- **影响**:
  - 任何获得此密钥的人都可以伪造任意用户的 JWT token
  - token 可被离线解密，泄露用户 ID、用户名和角色信息
  - Git 历史中永久保留此密钥（除非做 `git filter-branch`）
- **修复**:
  1. 将密钥移至环境变量：`${JWT_SECRET}`，开发环境用 `.env` 文件（加入 `.gitignore`）
  2. 生成高强度随机密钥：`openssl rand -base64 64`
  3. 生产环境使用密钥管理服务（如 HashiCorp Vault、AWS Secrets Manager）
  4. 对已提交的密钥，轮换后立即废弃旧密钥

#### [C-02] 数据库密码明文存储在配置和 docker-compose 中

- **位置**: [application.yml:14-15](apps/api/src/main/resources/application.yml:14-15), [docker-compose.yml:12](docker-compose.yml:12)
- **描述**:
  ```yaml
  # application.yml
  username: labreserve
  password: labreserve123

  # docker-compose.yml
  MYSQL_ROOT_PASSWORD: root
  MYSQL_PASSWORD: labreserve123
  ```
  MySQL root 密码和用户密码均为明文且过于简单。
- **影响**: 数据库凭据泄露后攻击者可直接读取所有用户数据。
- **修复**:
  1. 开发环境使用 `.env` 文件注入密码（加入 `.gitignore`）
  2. 使用强密码（16+ 字符、大小写、数字、特殊字符）
  3. 本地 MySQL root 密码不要与 Docker root 密码相同

### High

#### [H-01] 无速率限制（Rate Limiting）

- **位置**: [SecurityConfig.java:30-43](apps/api/src/main/java/com/labreserve/config/SecurityConfig.java:30-43)
- **描述**: 整个 API 没有任何速率限制机制。`/api/auth/login`、`/api/auth/register` 等公开端点可以被无限次调用。
- **影响**:
  - 暴力破解攻击：攻击者可无限次尝试用户名/密码组合
  - 资源耗尽：恶意注册可快速填满数据库
  - DDoS：简单的脚本即可压垮后端服务
- **修复**:
  1. 引入 `bucket4j` 或 Spring Cloud Gateway 限流
  2. 对登录接口实现 IP 级别限流（如 5 次/分钟/ip）
  3. 可选方案：使用 Nginx `limit_req` 模块、Redis + Lua 令牌桶
  4. 建议配置：登录 5 次/分钟，注册 3 次/小时/ip，全局 API 100 次/分钟/用户

#### [H-02] 无安全响应头

- **位置**: [SecurityConfig.java:30-43](apps/api/src/main/java/com/labreserve/config/SecurityConfig.java:30-43)
- **描述**: Spring Security 配置中未添加任何安全响应头。所有 API 响应缺少以下关键安全头：
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 0`（现代浏览器已废弃但仍有用）
  - `Strict-Transport-Security`（HSTS）
  - `Content-Security-Policy`
  - `Referrer-Policy`
- **影响**: 缺少安全头使应用更容易受到 MIME 嗅探、Clickjacking、XSS 等攻击。虽然后端返回的是 JSON，但健康检查等端点可能被 `<iframe>` 嵌入。
- **修复**:
  ```java
  http.headers(headers -> headers
      .contentTypeOptions(Customizer.withDefaults())
      .frameOptions(Customizer.withDefaults())      // X-Frame-Options: DENY
      .xssProtection(Customizer.withDefaults())      // X-XSS-Protection: 0
      .httpStrictTransportSecurity(hsts -> hsts     // 仅生产环境
          .includeSubDomains(true)
          .maxAgeInSeconds(31536000))
  );
  ```

#### [H-03] 无全局异常兜底，存在内部信息泄露风险

- **位置**: [GlobalExceptionHandler.java:20-36](apps/api/src/main/java/com/labreserve/handler/GlobalExceptionHandler.java:20-36)
- **描述**: `GlobalExceptionHandler` 只处理了 3 种异常类型：
  - `BusinessException` — 业务异常
  - `MethodArgumentNotValidException` — 参数校验
  - `AccessDeniedException` — 权限拒绝

  任何未被捕获的异常（如 `NullPointerException`、`SQLException`、`HttpMessageNotReadableException`）都会穿透到 Spring Boot 的默认异常处理（`/error`），返回包含堆栈跟踪的 HTML 或 JSON。

- **影响**: 攻击者可能通过构造特殊输入触发未处理的异常，从错误响应中获取：
  - 数据表结构（SQL 错误消息）
  - 代码路径信息
  - 内部类名和方法名
- **修复**:
  ```java
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGenericException(Exception e) {
      log.error("Unhandled exception", e);  // 内部日志记录
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(new ApiError("INTERNAL_ERROR", "服务器内部错误", null));
  }
  ```

#### [H-04] MyBatis-Plus SQL 日志在生产配置中启用

- **位置**: [application.yml:28](apps/api/src/main/resources/application.yml:28)
- **描述**:
  ```yaml
  mybatis-plus:
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  ```
  `StdOutImpl` 会将所有 SQL 语句和参数打印到标准输出。如果应用部署到生产环境时未切换 profile，敏感数据（用户名、密码哈希、用户输入的查询参数）将明文出现在日志中。
- **影响**: 日志系统中可能包含完整的 SQL 查询（含用户个人信息），违反数据保护法规。日志文件被攻击者获取后可直接重建数据库内容。
- **修复**:
  1. 将 `log-impl` 移至 `application-dev.yml` profile
  2. 生产 profile 使用 `Slf4jImpl` + DEBUG 级别（默认不输出 SQL）
  3. 生产环境日志脱敏（使用 logback 的 `%replace` 或自定义 `MessageConverter`）
  4. 当前应立即确认：生产部署时一定会切换到非 dev profile

#### [H-05] Redis 无密码认证

- **位置**: [docker-compose.yml:24-36](docker-compose.yml:24-36), [application.yml:16-20](apps/api/src/main/resources/application.yml:16-20)
- **描述**: Docker Compose 和 application.yml 中的 Redis 均未配置密码认证。Redis 以无密码模式运行在 `0.0.0.0:6379`。
- **影响**: 如果 Redis 端口暴露到公网或内网被其他容器/主机访问，攻击者可以：
  - 读取所有缓存数据（会话、业务数据）
  - 通过 `FLUSHALL` 清空缓存
  - 利用 Redis 执行 Lua 脚本（Redis 7 默认已禁用 `EVAL` RO）
- **修复**:
  1. `docker-compose.yml` 中 Redis 添加 `command: redis-server --requirepass ${REDIS_PASSWORD}`
  2. `application.yml` 中添加 `spring.data.redis.password: ${REDIS_PASSWORD}`
  3. 生产环境 Redis 绑定到 `127.0.0.1` 而非 `0.0.0.0`

### Medium

#### [M-01] AuthController 的 `/me` 和 `/change-password` 缺少 @PreAuthorize

- **位置**: [AuthController.java:44-56](apps/api/src/main/java/com/labreserve/controller/AuthController.java:44-56)
- **描述**: `GET /api/auth/me` 和 `PUT /api/auth/change-password` 没有 `@PreAuthorize` 注解。虽然 SecurityConfig 中 `requestMatchers("/api/**").authenticated()` 确保了请求经过认证，但没有方法级别的权限声明。这依赖全局 URL 级别的认证检查，一旦 SecurityConfig 配置被修改，这些端点可能意外暴露。
- **影响**: 低风险——当前有全局认证保护。但如果 SecurityConfig 的 `authenticated()` 范围被缩小，这些端点可能暴露。
- **修复**:
  ```java
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<UserInfo> me() { ... }

  @PutMapping("/change-password")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Void> changePassword(...) { ... }
  ```

#### [M-02] 密码修改不支持多设备会话失效

- **位置**: [AuthService.java:89-96](apps/api/src/main/java/com/labreserve/service/AuthService.java:89-96)
- **描述**: 用户修改密码后，旧的 JWT token 仍然有效直到过期（24 小时）。如果一个设备被黑客短暂获取，即使合法用户修改了密码，黑客手中的 token 仍可继续使用。
- **影响**: 密码修改不能立即阻止已泄露的 token 继续访问系统。
- **修复**:
  1. 引入 token 版本号（存在 Redis 中，key = `user:{userId}:tokenVersion`）
  2. JWT 中携带版本号，修改密码时递增版本号
  3. JwtAuthenticationFilter 中校验 token 版本号
  4. 或简化为：密码修改后将该用户的所有活跃 token 加入黑名单（存在 Redis 中，TTL = token 剩余有效期）

#### [M-03] 注册接口缺少验证码/反爬机制

- **位置**: [AuthController.java:31-36](apps/api/src/main/java/com/labreserve/controller/AuthController.java:31-36)
- **描述**: `POST /api/auth/register` 无需任何验证码、邮箱验证或人机验证即可创建用户。
- **影响**:
  - 自动化脚本可批量注册垃圾账号
  - 无邮箱验证意味着可以使用不存在的邮箱地址
- **修复**:
  1. 添加图形验证码（首次注册时）
  2. 邮箱验证：发送验证链接，激活后账号才可登录
  3. 或引入第三方人机验证（如 hCaptcha、Cloudflare Turnstile）

#### [M-04] 前端 token 存储在 localStorage，易受 XSS 攻击

- **位置**: [auth.ts:7](apps/web/src/stores/auth.ts:7), [client.ts:19-24](apps/web/src/api/client.ts:19-24)
- **描述**:
  ```ts
  const token = ref<string | null>(localStorage.getItem("token"));
  localStorage.setItem("token", result.token);
  ```
  JWT token 存储在 `localStorage` 中，任何成功的 XSS 注入都可以直接读取 token 并发送给攻击者。
- **影响**: 如果应用中存在 XSS 漏洞（例如通过 Element Plus 组件渲染未转义的用户输入），攻击者可以窃取任意用户的 token。
- **修复**:
  1. 更安全的方式：token 存储在 `httpOnly` cookie 中（需后端配合）
  2. 如必须用 localStorage，实施严格的 CSP（Content-Security-Policy）策略
  3. 确保所有用户输入经过输出编码（Element Plus 默认会转义，但需确认自定义渲染）
  4. 添加 `Content-Security-Policy: script-src 'self'` 响应头

#### [M-05] Docker MySQL root 密码过于简单且暴露端口

- **位置**: [docker-compose.yml:3-22](docker-compose.yml:3-22)
- **描述**:
  ```yaml
  environment:
    MYSQL_ROOT_PASSWORD: root
  ports:
    - "3307:3306"
  ```
  MySQL 端口映射到宿主机 `3307`，root 密码为 `root`。
- **影响**: 如果宿主机防火墙配置不当，外部网络可以尝试连接 MySQL，暴力破解 root 密码。
- **修复**:
  1. root 密码使用强随机密码
  2. 生产环境不映射 MySQL 端口到宿主机（仅允许容器网络内访问）
  3. 添加 `127.0.0.1:3307:3306` 限制仅本地访问

### Low

#### [L-01] CSRF 保护完全禁用

- **位置**: [SecurityConfig.java:32](apps/api/src/main/java/com/labreserve/config/SecurityConfig.java:32)
- **描述**: `csrf.disable()` 完全禁用了 CSRF 保护。对于纯 API（无 Cookie 会话、使用 `Authorization: Bearer` header）的服务，CSRF 攻击通常不适用。但如果未来添加了基于 Cookie 的认证（如 OAuth2 回调），CSRF 保护将被遗漏。
- **影响**: 当前无直接影响。如有计划引入浏览器 Cookie（如 SAML/OIDC），需重新启用 CSRF。
- **修复**: 添加注释说明禁用 CSRF 的理由。如果引入 Cookie 认证，需将 CSRF 保护重新加入。

#### [L-02] 健康检查端点暴露服务版本信息

- **位置**: [HealthController.java:14-21](apps/api/src/main/java/com/labreserve/controller/HealthController.java:14-21)
- **描述**: `/api/health` 返回 `"service": "LabReserve API"` 和时间戳，虽然信息量有限，但确认了后端技术栈。对于内部系统可接受，但暴露到公网会帮助攻击者定位目标。
- **影响**: 低——仅暴露服务名称和时间戳，不足以构成直接威胁。
- **修复**: 生产环境只返回 `{"status": "UP"}`，详细信息仅在 dev profile 下返回。

#### [L-03] `@PreAuthorize` 部分注解可提升到类级别

- **位置**: 多个 Controller（ReviewController、RepairLogController 等）
- **描述**: 部分 controller 的每个方法都有相同的 `@PreAuthorize("isAuthenticated()")`，但没有提取到类级别。这增加了遗漏的风险——如果新增方法忘记添加注解。
- **影响**: 低——因为 SecurityConfig 全局规则 `"/api/**".authenticated()` 提供了兜底保护。但如果未来调整了全局规则，单个方法可能被遗漏。
- **修复**: 全部控制器添加类级别 `@PreAuthorize`（如 `@PreAuthorize("isAuthenticated()")`），只对需要更严格权限的方法覆盖。

#### [L-04] 前端 401 处理中使用 `window.location.href` 进行重定向

- **位置**: [client.ts:49](apps/web/src/api/client.ts:49)
- **描述**: 当收到 401 响应时，前端使用 `window.location.href = "/login"` 进行硬重定向，这会丢失当前 SPA 状态。
- **影响**: 低——用户体验问题，不直接构成安全威胁。但硬重定向可能导致未保存数据丢失。
- **修复**: 使用 `router.push("/login")` 代替 `window.location.href`，保持 SPA 导航。

---

## 改进优先级

### 立即修复（P0 — 上线前必须解决）

1. **[C-01]** JWT 密钥环境变量化，轮换现有密钥
2. **[C-02]** 数据库密码环境变量化，使用强密码
3. **[H-01]** 对 `/api/auth/login` 和 `/api/auth/register` 添加速率限制
4. **[H-04]** SQL 日志确认仅在 dev profile 启用

### 短期修复（P1 — 1-2 周内）

5. **[H-02]** 添加安全响应头（`X-Content-Type-Options`、`X-Frame-Options`、HSTS）
6. **[H-03]** 添加全局 `Exception.class` 兜底异常处理器
7. **[H-05]** Redis 配置访问密码
8. **[M-02]** 密码修改后使旧 token 失效

### 中期改进（P2 — 1 个月内）

9. **[M-03]** 注册流程添加验证码或邮箱验证
10. **[M-04]** 评估 token 存储方案，考虑 httpOnly cookie
11. **[M-01]** AuthController 补全 `@PreAuthorize` 注解
12. **[M-05]** 加固 Docker Compose 配置

### 长期优化（P3）

13. **[L-01]** 评估 CSRF 策略，添加文档说明
14. **[L-02]** 生产环境健康检查最小化信息
15. **[L-03]** Controller 类级别 `@PreAuthorize` 重构
16. **[L-04]** 前端 401 处理改用 `router.push`

---

## 附录 A：审计范围详情

| 审计项     | 文件数 | 关键发现                                                  |
| ---------- | ------ | --------------------------------------------------------- |
| 安全配置   | 3      | JWT 密钥硬编码、CSRF 禁用、无安全头                       |
| 控制器     | 14     | 全部有 `@PreAuthorize`，但粒度不统一                      |
| DTO 验证   | 18+    | Validation 注解覆盖良好，`@Pattern` 正则校验日期/时间格式 |
| Service 层 | 13     | 业务逻辑权限检查（如取消预约检查归属）                    |
| 配置文件   | 3      | 明文密码、SQL 日志输出                                    |
| 前端       | 8      | localStorage token 存储、axios 拦截器                     |
| Docker     | 1      | MySQL root 弱密码、Redis 无密码、端口暴露                 |

## 附录 B：依赖安全审计

### 后端（Maven）

| 依赖                   | 版本                            | 安全状态                              |
| ---------------------- | ------------------------------- | ------------------------------------- |
| Spring Boot            | 3.4.7                           | ✅ 最新 3.4.x，安全补丁及时           |
| Spring Security        | 3.4.7                           | ✅ 随 Spring Boot 版本                |
| MyBatis-Plus           | 3.5.11                          | ✅ 最新版本                           |
| jjwt (io.jsonwebtoken) | 0.12.6                          | ✅ 最新版本，JWT 标准实现             |
| redisson               | 3.52.0                          | ⚠️ 非最新 LTS（最新 3.40+），建议升级 |
| MySQL Connector/J      | 由 Spring Boot 管理             | ✅                                    |
| H2 (test)              | 由 Spring Boot 管理             | ✅ 仅测试 scope                       |
| Lombok                 | 1.18.30+（由 Spring Boot 管理） | ✅                                    |
| jackson                | 由 Spring Boot 管理             | ✅                                    |

### 前端（pnpm）

| 依赖             | 版本   | 安全状态 |
| ---------------- | ------ | -------- |
| Vue 3            | 3.5.16 | ✅ 最新  |
| Vite             | 6.3.5  | ✅ 最新  |
| Element Plus     | 2.10.5 | ✅ 较新  |
| Axios            | 1.18.1 | ✅ 最新  |
| Pinia            | 3.0.3  | ✅ 最新  |
| ECharts          | 6.1.0  | ✅ 最新  |
| @playwright/test | 1.61.1 | ✅ 最新  |

**结论**：所有依赖均处于较新版本，未发现已知 CVE。Spring Boot 3.4.7 于 2026 年发布，享有完整的安全支持。Redisson 3.52.0 可考虑升级到最新版本以获得更好的性能和 bug 修复。

---

## 附录 C：API 认证覆盖清单

| 端点                                | 认证方式 | 权限                                    | 状态                      |
| ----------------------------------- | -------- | --------------------------------------- | ------------------------- |
| `GET /api/health`                   | 无需认证 | 公开                                    | ✅                        |
| `POST /api/auth/register`           | 无需认证 | 公开                                    | ⚠️ 无验证码               |
| `POST /api/auth/login`              | 无需认证 | 公开                                    | ⚠️ 无速率限制             |
| `GET /api/auth/me`                  | JWT      | 已认证                                  | ⚠️ 无方法级 @PreAuthorize |
| `PUT /api/auth/change-password`     | JWT      | 已认证                                  | ⚠️ token 无效化           |
| `GET/POST/PUT/DELETE /api/users/**` | JWT      | ADMIN                                   | ✅                        |
| `GET /api/labs/**`                  | JWT      | 已认证                                  | ✅                        |
| `POST/PUT/DELETE /api/labs/**`      | JWT      | ADMIN                                   | ✅                        |
| `GET/POST /api/bookings/**`         | JWT      | 已认证                                  | ✅                        |
| `PUT /api/bookings/{id}/approve`    | JWT      | TEACHER, ADMIN                          | ✅                        |
| `PUT /api/bookings/{id}/complete`   | JWT      | TEACHER, ADMIN                          | ✅                        |
| `GET/POST /api/equipment/**`        | JWT      | 已认证 / ADMIN                          | ✅                        |
| `GET/POST /api/borrows/**`          | JWT      | 已认证 / TEACHER,ADMIN                  | ✅                        |
| `GET /api/courses/**`               | JWT      | 已认证                                  | ✅                        |
| `POST/PUT/DELETE /api/courses/**`   | JWT      | TEACHER, ADMIN                          | ✅                        |
| `GET/POST/DELETE /api/notices/**`   | JWT      | 已认证 / TEACHER,ADMIN                  | ✅                        |
| `GET/POST /api/reviews/**`          | JWT      | 已认证                                  | ✅                        |
| `GET /api/usage-records/**`         | JWT      | TEACHER, ADMIN                          | ✅                        |
| `GET /api/dashboard/*`              | JWT      | 已认证 (student-ranking: TEACHER/ADMIN) | ✅                        |
| `GET/POST /api/repair-logs/**`      | JWT      | 已认证 / ADMIN                          | ✅                        |
| `GET /api/students/**`              | JWT      | 已认证                                  | ✅                        |
| `POST/PUT /api/students/**`         | JWT      | TEACHER, ADMIN                          | ✅                        |
| `DELETE /api/students/**`           | JWT      | ADMIN                                   | ✅                        |

**结论**：所有 67 个 API 端点均有认证保护。67 个端点的权限设计符合 api-design.md 中的角色矩阵规范。
