# LabReserve 部署指南

> 版本：1.0 | 日期：2026-07-02 | 适用环境：staging / production

---

## 目录

1. [部署架构概览](#一部署架构概览)
2. [环境要求](#二环境要求)
3. [环境变量说明](#三环境变量说明)
4. [Docker Compose 部署](#四docker-compose-部署)
5. [手动部署](#五手动部署)
6. [Nginx 配置](#六nginx-配置)
7. [健康检查与监控](#七健康检查与监控)
8. [数据备份](#八数据备份)
9. [回滚方案](#九回滚方案)
10. [常见问题](#十常见问题)

---

## 一、部署架构概览

```
                    ┌──────────┐
                    │  Nginx   │  :80/:443
                    └────┬─────┘
                         │
              ┌──────────┼──────────┐
              │          │          │
         /api/*     /assets/*   / (SPA)
              │          │          │
      ┌───────┴──────┐   │    ┌─────┴──────┐
      │ Spring Boot  │   │    │ Vue 3 SPA  │
      │   :8080      │   │    │  静态文件   │
      └───────┬──────┘   │    └────────────┘
              │          │
    ┌─────────┼──────────┤
    │         │          │
┌───┴───┐ ┌───┴───┐ ┌───┴───┐
│ MySQL │ │ Redis │ │ NFS/  │
│  3306 │ │  6379 │ │ OSS   │
└───────┘ └───────┘ └───────┘
```

- **Nginx**：反向代理 + SSL 终止 + 静态资源服务
- **Spring Boot**：后端 API 服务（支持水平扩展，stateless）
- **MySQL**：主数据库（建议主从复制或集群）
- **Redis**：缓存 + 分布式锁 + token 版本管理
- **Vue SPA**：静态文件部署在 Nginx 或 CDN

---

## 二、环境要求

| 组件               | 最低版本 | 推荐             |
| ------------------ | -------- | ---------------- |
| Docker             | 26+      | 最新稳定版       |
| Docker Compose     | v2       | v2.30+           |
| Java (手动部署)    | 17       | 17 LTS (Temurin) |
| Maven (手动部署)   | 3.9      | 3.9+             |
| Node.js (手动部署) | 20       | 20 LTS           |
| pnpm (手动部署)    | 9        | 9+               |
| MySQL              | 8.0      | 8.0.35+          |
| Redis              | 7        | 7.2+             |
| Nginx              | 1.25     | 1.27+            |

---

## 三、环境变量说明

### 后端（Spring Boot）

| 变量                     | 必填      | 默认值 (dev)                                 | 说明                                     |
| ------------------------ | --------- | -------------------------------------------- | ---------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | ✅        | `dev`                                        | 环境：`dev` / `prod`                     |
| `JWT_SECRET`             | ✅        | —                                            | JWT HMAC-SHA256 签名密钥（至少 32 字节） |
| `MYSQL_URL`              | ✅ (prod) | `jdbc:mysql://localhost:3306/labreserve_dev` | 数据库 JDBC URL                          |
| `MYSQL_USERNAME`         | ✅ (prod) | `labreserve`                                 | 数据库用户名                             |
| `MYSQL_PASSWORD`         | ✅ (prod) | —                                            | 数据库密码                               |
| `REDIS_HOST`             | ✅ (prod) | `localhost`                                  | Redis 主机地址                           |
| `REDIS_PASSWORD`         | ✅ (prod) | —                                            | Redis 密码                               |
| `SERVER_PORT`            | ❌        | `8080`                                       | 服务端口                                 |

### 前端（Vite Build）

前端构建时不读取环境变量（build-time 而非 runtime）。API base URL 通过 Nginx 反向代理处理，无需前端环境变量。

### Docker Compose

| 变量                  | 必填 | 默认值          | 说明            |
| --------------------- | ---- | --------------- | --------------- |
| `MYSQL_ROOT_PASSWORD` | ❌   | `root`          | MySQL root 密码 |
| `MYSQL_PASSWORD`      | ❌   | `labreserve123` | MySQL 用户密码  |
| `REDIS_PASSWORD`      | ❌   | `redis-dev`     | Redis 密码      |

### 生成随机密钥

```bash
# JWT 密钥（64 字节 base64）
openssl rand -base64 64

# MySQL 密码（32 字符随机字符串）
openssl rand -hex 16

# Redis 密码
openssl rand -hex 16
```

---

## 四、Docker Compose 部署

### 4.1 生产 Compose 文件

创建 `docker-compose.prod.yml`：

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.0
    container_name: labreserve-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: labreserve
      MYSQL_USER: ${MYSQL_USERNAME}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - labreserve
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: labreserve-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    networks:
      - labreserve
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    image: labreserve-api:${IMAGE_TAG:-latest}
    container_name: labreserve-api
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JWT_SECRET: ${JWT_SECRET}
      MYSQL_URL: jdbc:mysql://mysql:3306/labreserve?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      MYSQL_USERNAME: ${MYSQL_USERNAME}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    ports:
      - "8080:8080"
    networks:
      - labreserve
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  nginx:
    image: nginx:1.27-alpine
    container_name: labreserve-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./dist:/usr/share/nginx/html:ro
      - ./ssl:/etc/nginx/ssl:ro
    networks:
      - labreserve
    depends_on:
      - api

volumes:
  mysql_data:
  redis_data:

networks:
  labreserve:
    driver: bridge
```

### 4.2 构建与启动

```bash
# 1. 构建前端
pnpm --filter @labreserve/web build
# 输出在 apps/web/dist/

# 2. 构建后端 Docker 镜像
cd apps/api
mvn clean package -DskipTests
docker build -t labreserve-api:latest .

# 3. 设置环境变量
export JWT_SECRET=$(openssl rand -base64 64)
export MYSQL_ROOT_PASSWORD=$(openssl rand -hex 16)
export MYSQL_USERNAME=labreserve
export MYSQL_PASSWORD=$(openssl rand -hex 16)
export REDIS_PASSWORD=$(openssl rand -hex 16)
export IMAGE_TAG=latest

# 4. 启动所有服务
docker compose -f docker-compose.prod.yml up -d

# 5. 验证
curl http://localhost/api/health
```

### 4.3 常用运维命令

```bash
# 查看服务状态
docker compose -f docker-compose.prod.yml ps

# 查看日志
docker compose -f docker-compose.prod.yml logs -f api
docker compose -f docker-compose.prod.yml logs --tail=100 mysql

# 重启单个服务
docker compose -f docker-compose.prod.yml restart api

# 水平扩展 API（需要去掉固定端口映射或加负载均衡）
docker compose -f docker-compose.prod.yml up -d --scale api=2

# 停止所有服务
docker compose -f docker-compose.prod.yml down
```

---

## 五、手动部署

### 5.1 后端（JAR 包部署）

```bash
# 1. 编译打包
cd apps/api
mvn clean package -DskipTests -Pprod

# 2. 上传到服务器
scp target/labreserve-api-1.0.0-SNAPSHOT.jar user@server:/opt/labreserve/

# 3. 设置环境变量并启动
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=<your-secret>
export MYSQL_URL=jdbc:mysql://localhost:3306/labreserve?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
export MYSQL_USERNAME=labreserve
export MYSQL_PASSWORD=<your-password>
export REDIS_HOST=localhost
export REDIS_PASSWORD=<your-redis-password>

java -jar -Xms512m -Xmx2g target/labreserve-api-1.0.0-SNAPSHOT.jar
```

### 5.2 前端（静态文件）

```bash
# 1. 安装依赖并构建
pnpm install --frozen-lockfile
pnpm --filter @labreserve/web build

# 2. 部署到 Nginx 静态目录
rsync -avz apps/web/dist/ user@server:/usr/share/nginx/html/
```

### 5.3 使用 systemd 管理后端进程

创建 `/etc/systemd/system/labreserve-api.service`：

```ini
[Unit]
Description=LabReserve API Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=labreserve
WorkingDirectory=/opt/labreserve
EnvironmentFile=/opt/labreserve/.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar labreserve-api.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable labreserve-api
sudo systemctl start labreserve-api
sudo systemctl status labreserve-api
```

---

## 六、Nginx 配置

```nginx
# /etc/nginx/nginx.conf 或 /etc/nginx/sites-available/labreserve

upstream labreserve_api {
    server 127.0.0.1:8080;
    # 多实例时添加:
    # server 127.0.0.1:8081;
    # server 127.0.0.1:8082;
    keepalive 32;
}

server {
    listen 80;
    server_name labreserve.your-domain.edu.cn;

    # 强制 HTTPS (生产环境)
    # return 301 https://$host$request_uri;

    # 前端静态资源
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
    gzip_min_length 1000;

    # 缓存策略
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://labreserve_api;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;

        # 速率限制 (可选)
        # limit_req zone=api_limit burst=20 nodelay;
    }

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 安全响应头
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 日志
    access_log /var/log/nginx/labreserve-access.log;
    error_log /var/log/nginx/labreserve-error.log;
}

# 速率限制区域定义
# limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
```

---

## 七、健康检查与监控

### 7.1 健康检查端点

| 端点                       | 说明         | 预期响应                                         |
| -------------------------- | ------------ | ------------------------------------------------ |
| `GET /api/health`          | 综合健康检查 | `{"status":"UP","service":"LabReserve API",...}` |
| `GET /api/health` 返回 5xx | 服务不可用   | —                                                |

### 7.2 使用 curl 验证

```bash
# 基础健康检查
curl http://localhost:8080/api/health

# 检查完整响应
curl -s http://localhost:8080/api/health | jq .

# 作为 Docker HEALTHCHECK
# docker-compose.yml:
#   healthcheck:
#     test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
#     interval: 30s
#     timeout: 5s
#     retries: 3
```

### 7.3 监控指标

| 指标             | 监控方式                | 告警阈值       |
| ---------------- | ----------------------- | -------------- |
| API 响应时间     | Prometheus / Micrometer | P95 > 2s       |
| JVM 堆内存使用率 | JMX / Micrometer        | > 80%          |
| MySQL 连接数     | MySQL Exporter          | > 80% 最大连接 |
| Redis 命中率     | Redis Exporter          | < 80%          |
| 磁盘使用率       | Node Exporter           | > 85%          |
| Nginx 错误率     | Nginx logs              | > 5%           |

---

## 八、数据备份

### 8.1 MySQL 备份

```bash
#!/bin/bash
# backup-mysql.sh

BACKUP_DIR="/opt/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

mkdir -p $BACKUP_DIR

# 全量备份
docker exec labreserve-mysql mysqldump \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  --single-transaction \
  --routines \
  --triggers \
  labreserve | gzip > $BACKUP_DIR/labreserve_${DATE}.sql.gz

# 清理过期备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: labreserve_${DATE}.sql.gz"
```

### 8.2 定时备份（crontab）

```bash
# 每天凌晨 2:00 执行备份
0 2 * * * /opt/labreserve/scripts/backup-mysql.sh >> /var/log/labreserve-backup.log 2>&1
```

### 8.3 恢复数据库

```bash
# 恢复最新备份
LATEST_BACKUP=$(ls -t /opt/backups/mysql/labreserve_*.sql.gz | head -1)
gunzip < $LATEST_BACKUP | docker exec -i labreserve-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} labreserve
```

---

## 九、回滚方案

### 9.1 Docker Compose 回滚

```bash
# 方式 1：回滚到指定版本的镜像
export IMAGE_TAG=v1.2.3  # 上一个稳定版本
docker compose -f docker-compose.prod.yml up -d api

# 方式 2：Docker 镜像打标签策略
# 部署前：给当前镜像打备份标签
docker tag labreserve-api:latest labreserve-api:previous

# 部署新版本后如有问题，一键回退
docker tag labreserve-api:previous labreserve-api:latest
docker compose -f docker-compose.prod.yml up -d api
```

### 9.2 JAR 包回滚

```bash
# 保留最近 3 个版本
ls -t /opt/labreserve/labreserve-api-*.jar | tail -n +4 | xargs rm -f

# 回滚到上一个版本
ROLLBACK_JAR=$(ls -t /opt/labreserve/labreserve-api-*.jar | sed -n '2p')
sudo systemctl stop labreserve-api
cp $ROLLBACK_JAR /opt/labreserve/labreserve-api.jar
sudo systemctl start labreserve-api
```

### 9.3 数据库回滚

```bash
# 方式 1：从备份恢复（见 8.3）
# 方式 2：Flyway/Liquibase 迁移回滚（需要配置）
# 方式 3：MySQL binlog 时间点恢复
mysqlbinlog --stop-datetime="2026-07-02 10:00:00" binlog.000001 | mysql -u root -p
```

### 9.4 回滚检查清单

- [ ] 确认回滚版本号 / 备份时间点
- [ ] 通知团队进入维护模式
- [ ] 执行回滚操作
- [ ] 验证健康检查：`curl /api/health`
- [ ] 验证核心流程：登录 → 查看实验室 → 创建预约
- [ ] 通知团队回滚完成
- [ ] 分析根因并记录到 `docs/bug/`

---

## 十、常见问题

### Q: 启动后端时提示 `Access denied for user 'labreserve'`

**A**: 检查 `MYSQL_PASSWORD` 是否正确。如果是首次部署，确保 MySQL 已执行 `init.sql` 初始化脚本。手动创建用户：

```sql
CREATE USER IF NOT EXISTS 'labreserve'@'%' IDENTIFIED BY '<password>';
CREATE DATABASE IF NOT EXISTS labreserve CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON labreserve.* TO 'labreserve'@'%';
FLUSH PRIVILEGES;
```

### Q: Redis 连接失败

**A**: 检查 `REDIS_PASSWORD` 是否正确。Docker 环境下 Redis 容器内互相通信使用容器名（`redis`）而非 `localhost`：

```bash
docker compose -f docker-compose.prod.yml exec redis redis-cli -a $REDIS_PASSWORD ping
# 应返回 PONG
```

### Q: API 返回 500 Internal Error

**A**: 查看日志定位问题：

```bash
docker compose -f docker-compose.prod.yml logs api | tail -100
systemctl status labreserve-api && journalctl -u labreserve-api -f
```

常见原因：数据库未就绪（等待 healthcheck 通过）、JWT_SECRET 未设置、Redis 连接失败。

### Q: 前端页面空白或路由不工作

**A**: 确保 Nginx 配置了 SPA 回退规则：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Q: CORS 错误

**A**: 生产环境应通过 Nginx 同源代理避免 CORS 问题。开发环境已在 `vite.config.ts` 配置代理。如果直接访问不同端口，需在 `SecurityConfig.java` 配置 CORS：

```java
// 不推荐生产环境使用
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

---

## 附录：快速部署检查清单

### 首次部署

- [ ] 生成所有密钥（JWT_SECRET, MYSQL_PASSWORD, REDIS_PASSWORD）
- [ ] 配置 DNS 指向服务器 IP
- [ ] 安装 Docker / Docker Compose / Nginx
- [ ] 拉取代码：`git clone`
- [ ] 配置 `.env` 文件（所有必填环境变量）
- [ ] 构建镜像：`docker build`
- [ ] 启动服务：`docker compose up -d`
- [ ] 执行数据库初始化：运行 `init.sql`
- [ ] 配置 Nginx + SSL 证书（Let's Encrypt）
- [ ] 验证所有端点：`curl /api/health`
- [ ] 测试核心流程（注册 → 登录 → 预约）
- [ ] 配置备份：MySQL dump 定时任务
- [ ] 配置监控告警

### 每次更新部署

- [ ] 查看 git log 确认变更范围
- [ ] 阅读 CHANGELOG（如有）
- [ ] 运行测试：`mvn test && pnpm test`
- [ ] 构建新版本：`mvn package && pnpm build`
- [ ] 备份当前版本（镜像 tag / JAR 文件）
- [ ] 执行部署（滚动更新或 blue-green）
- [ ] 验证健康检查
- [ ] 验证核心流程
- [ ] 监控日志 5 分钟确认无异常
