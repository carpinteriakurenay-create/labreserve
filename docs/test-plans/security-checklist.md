# 安全测试清单

> M4-13 回归测试 | 2026-07-01

## A. 权限提升测试 (Privilege Escalation)

| #   | 测试场景                      | 请求                                 | 预期结果      | 通过 |
| --- | ----------------------------- | ------------------------------------ | ------------- | ---- |
| A1  | Student 访问 Admin 用户管理   | `GET /api/users`                     | 403 FORBIDDEN | [ ]  |
| A2  | Student 访问 Admin 实验室创建 | `POST /api/labs`                     | 403 FORBIDDEN | [ ]  |
| A3  | Student 访问 Admin 设备管理   | `POST /api/equipment`                | 403 FORBIDDEN | [ ]  |
| A4  | Student 访问 Teacher 待审批   | `GET /api/bookings/pending`          | 403 FORBIDDEN | [ ]  |
| A5  | Student 访问 Teacher 使用记录 | `GET /api/usage-records`             | 403 FORBIDDEN | [ ]  |
| A6  | Student 访问学生排行          | `GET /api/dashboard/student-ranking` | 403 FORBIDDEN | [ ]  |
| A7  | Teacher 访问 Admin 用户管理   | `GET /api/users`                     | 403 FORBIDDEN | [ ]  |
| A8  | Teacher 访问 Admin 实验室创建 | `POST /api/labs`                     | 403 FORBIDDEN | [ ]  |
| A9  | Teacher 删除学生信息          | `DELETE /api/students/{id}`          | 403 FORBIDDEN | [ ]  |
| A10 | Student 修改报修状态          | `PUT /api/repair-logs/{id}/status`   | 403 FORBIDDEN | [ ]  |

## B. 认证测试 (Authentication)

| #   | 测试场景                | 请求                   | 预期结果         | 通过 |
| --- | ----------------------- | ---------------------- | ---------------- | ---- |
| B1  | 无 Token 访问受保护接口 | `GET /api/labs`        | 401 UNAUTHORIZED | [ ]  |
| B2  | 无效 Token              | Bearer 伪造值          | 401 UNAUTHORIZED | [ ]  |
| B3  | 过期 Token              | 已过期 JWT             | 401 UNAUTHORIZED | [ ]  |
| B4  | 被篡改的 JWT            | 修改 payload 后        | 401 UNAUTHORIZED | [ ]  |
| B5  | 缺失 Bearer 前缀        | `Authorization: token` | 401 UNAUTHORIZED | [ ]  |
| B6  | 错误的签名密钥          | 用另一密钥签发         | 401 UNAUTHORIZED | [ ]  |
| B7  | 健康检查无需认证        | `GET /api/health`      | 200 OK           | [ ]  |

## C. XSS 注入测试

测试字段: purpose (预约), description (报修), comment (评价), content (通知), title (通知), name (实验室)

| #   | 测试向量                        | 测试接口                            | 预期结果         | 通过 |
| --- | ------------------------------- | ----------------------------------- | ---------------- | ---- |
| C1  | `<script>alert('xss')</script>` | POST /api/bookings (purpose)        | 200/201 正常处理 | [ ]  |
| C2  | `<script>alert('xss')</script>` | POST /api/reviews (comment)         | 200/201 正常处理 | [ ]  |
| C3  | `<script>alert('xss')</script>` | POST /api/repair-logs (description) | 200/201 正常处理 | [ ]  |
| C4  | `<img src=x onerror=alert(1)>`  | POST /api/reviews (comment)         | 200/201 正常处理 | [ ]  |
| C5  | `<script>alert(1)</script>`     | POST /api/notices (content)         | 200/201 正常处理 | [ ]  |
| C6  | `<script>alert(1)</script>`     | POST /api/notices (title)           | 200/201 正常处理 | [ ]  |

> 说明: 后端返回 JSON，前端 Vue 模板自动转义。后端应接受文本输入并按原样存储。

## D. SQL 注入测试

| #   | 测试向量                         | 测试接口                     | 预期结果           | 通过 |
| --- | -------------------------------- | ---------------------------- | ------------------ | ---- |
| D1  | `' OR '1'='1`                    | GET /api/labs?name=          | 空结果或无影响     | [ ]  |
| D2  | `'; DROP TABLE bookings;--`      | POST /api/bookings (purpose) | 200/201 按文本保存 | [ ]  |
| D3  | `1' UNION SELECT * FROM users--` | GET /api/users?role=         | 400 或 空结果      | [ ]  |

## E. IDOR (不安全直接对象引用) 测试

| #   | 测试场景                       | 请求                                 | 预期结果         | 通过 |
| --- | ------------------------------ | ------------------------------------ | ---------------- | ---- |
| E1  | Student 访问其他用户的预约详情 | `GET /api/bookings/{otherUserId}`    | 200 但应校验归属 | [ ]  |
| E2  | Student 取消其他用户的预约     | `PUT /api/bookings/{otherId}/cancel` | 403 FORBIDDEN    | [ ]  |
| E3  | Student 删除其他用户的评价     | `DELETE /api/reviews/{otherId}`      | 403 FORBIDDEN    | [ ]  |
| E4  | Student 修改其他用户的借用申请 | `PUT /api/borrows/{otherId}`         | 403/404          | [ ]  |

## F. 输入边界值测试

| #   | 测试场景                | 测试接口                         | 预期结果             | 通过 |
| --- | ----------------------- | -------------------------------- | -------------------- | ---- |
| F1  | 空字符串提交必填字段    | POST /api/auth/register          | 400 VALIDATION_ERROR | [ ]  |
| F2  | 评价评分超过最大值      | POST /api/reviews (rating=6)     | 400 VALIDATION_ERROR | [ ]  |
| F3  | 评价评分低于最小值      | POST /api/reviews (rating=0)     | 400 VALIDATION_ERROR | [ ]  |
| F4  | 无效的枚举值            | GET /api/bookings?status=INVALID | 400/409              | [ ]  |
| F5  | 超长字符串 (10000 字符) | POST /api/notices (content)      | 400 VALIDATION_ERROR | [ ]  |
| F6  | 负数作为分页参数        | /api/labs?pageNum=-1             | 400 或返回默认值     | [ ]  |

---

## 测试结果汇总

- 测试日期: _________
- 测试人员: _________
- 总计: _____ / 通过: _____ / 失败: _____ / 跳过: _____
- 是否发现安全漏洞: 是 / 否
- 备注: _________
