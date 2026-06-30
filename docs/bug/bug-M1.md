# M1 Bug 修复记录

> 日期：2026-06-29 ~ 2026-06-30

## Bug #1: M1-5/M1-6 认证接口 404 + 数据库连接失败

## 现象

两次测试认证接口均返回 404：

```
POST /api/auth/register → 404 Not Found
POST /api/auth/login    → 404 Not Found
```

## 根因分析

共 3 层问题，逐层暴露：

| 层  | 根因                                                                                                                                    | 现象                                                    |
| --- | --------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| 1   | **旧 Java 进程未重启** — `spring-boot:run` 进程在 M1-6 代码编译前就已运行，JVM 加载的是旧 `target/classes`（无 `AuthController.class`） | 404（DispatcherServlet 找不到 `/api/auth/*` 映射）      |
| 2   | **MySQL `labreserve` 用户不存在** — `init.sql` 依赖 Docker 执行，但本地 Docker Desktop 未运行，`init.sql` 从未被加载到本地 MySQL        | 500 → `Access denied for user 'labreserve'@'localhost'` |
| 3   | **init.sql 字符集问题** — 种子数据含中文字符（"张三"），而本地 MySQL 客户端 `character_set_client=gbk`，导致中文在 VARCHAR(50) 超长     | 500 → `Data too long for column 'real_name'`            |

### 根本原因总结

1. M1-6 新增 `AuthController` 后未重启 Spring Boot
2. `init.sql` 仅设计了 Docker 环境的数据库初始化路径，未覆盖本地 MySQL（端口 3306）的场景
3. `init.sql` 缺少 `SET NAMES utf8mb4;`，种子数据使用中文在 GBK 客户端连接下插入失败

## 修复方案

### 修复 1：重启 Spring Boot（每次编译后必须操作）

```bash
taskkill /F /IM java.exe
mvn spring-boot:run
```

### 修复 2：通过 MySQL root 创建用户和数据库

MySQL root 密码为 `123456`，执行：

```sql
CREATE USER IF NOT EXISTS 'labreserve'@'%' IDENTIFIED BY 'labreserve123';
CREATE DATABASE IF NOT EXISTS labreserve_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON labreserve_dev.* TO 'labreserve'@'%';
FLUSH PRIVILEGES;
```

### 修复 3：init.sql 增加字符集声明 + 种子数据用英文名

**文件**：`docker/mysql/init.sql`

改动一：在种子数据 INSERT 前加 `SET NAMES utf8mb4;`

改动二：种子用户名改为 ASCII：

```sql
INSERT INTO users (username, real_name, password, email, phone, role) VALUES
  ('2021001', 'Zhang San',   '...', 'zhangsan@univ.edu.cn', '13800001111', 'STUDENT'),
  ('T001',    'Prof. Li',    '...', 'lijs@univ.edu.cn',     '13900002222', 'TEACHER'),
  ('admin',   'Admin Wang',  '...', 'admin@univ.edu.cn',    '13700000000', 'ADMIN');
```

## 验证

修复后重新启动并测试，4 个端点全部通过：

```
POST /api/auth/register        → 201  {"code":"SUCCESS","message":"注册成功",...}
POST /api/auth/login           → 200  {"code":"SUCCESS",...,"data":{"token":"...","user":{...}}}
GET  /api/auth/me              → 200  {"code":"SUCCESS",...,"data":{...}}
PUT  /api/auth/change-password → 200  {"code":"SUCCESS","message":"密码修改成功",...}
```

## 经验教训

1. 每次 `mvn compile` 后，如果 Spring Boot 已在运行，**必须重启**才能加载新类
2. `init.sql` 应同时兼容 Docker 和本地 MySQL 两种环境
3. 种子数据优先用 ASCII 字符，避免 `character_set_client` 不匹配导致的插入失败；或在文件顶部声明 `SET NAMES utf8mb4;`

---

## Bug #2: init.sql 预生成 BCrypt 哈希无效

> 日期：2026-06-30 | 影响范围：M1-5 种子用户登录、M1-7 验证流程

### 现象

```
POST /api/auth/login {"username":"admin","password":"admin123"} → 401 INVALID_CREDENTIALS
POST /api/auth/login {"username":"2021001","password":"password123"} → 401 INVALID_CREDENTIALS
```

`docker/mysql/init.sql` 中预生成的 BCrypt 哈希 `$2b$10$...` 无法通过 Spring Security `BCryptPasswordEncoder.matches()` 校验，依赖 `init.sql` 种子数据的 admin 用户无法登录。

### 根因

`init.sql` 中的哈希值是硬编码的字面量，**从未经过 `BCryptPasswordEncoder.encode()` 生成**，与明文密码不匹配。Spring Security 的 `BCryptPasswordEncoder` 每次加密使用随机 salt，必须通过 API 端 `PasswordEncoder` 生成的哈希才是有效的。

```sql
-- init.sql 中的硬编码哈希（无法匹配原文）
INSERT INTO users (username, password) VALUES
  ('2021001', '$2b$10$gmiBQqb873lHaAHvTlxt6uymWuCIWOyk4QN9Nrk8yumfMY2TS/cNm'),  -- password123 → ✗
  ('admin',   '$2b$10$cCla6HDGcbFxx7YPHfmqUeIUtcY6nuCEvQh12nK.npiADZPmPAWrm');  -- admin123   → ✗
```

### 修复方案

**临时**：通过 API 注册用户 → 从 DB 提取真实哈希 → 更新 `init.sql`。

**永久**：在 `init.sql` 中添加注释说明哈希需用 `BCryptPasswordEncoder` 工具类生成，或在 CI/Docker 启动脚本中通过 Java 程序预生成哈希后再写入 `init.sql`。

### 验证

```bash
# API 注册的用户可正常登录
curl -s -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"devadmin","password":"admin123","realName":"Dev Admin"}'
# → 201 注册成功

curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"devadmin","password":"admin123"}'
# → 200 登录成功，返回有效 token
```

### 经验教训

1. `BCrypt` 每次加密 salt 不同，**哈希必须由编码器生成**，不能手动编写
2. 种子数据的初始化流程应包含一个 `BCryptPasswordEncoder` 调用环节，而非静态 SQL
3. 本地开发环境的首次启动脚本应包含"注册初始 ADMIN 用户"步骤
