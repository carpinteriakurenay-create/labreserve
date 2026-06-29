# LabReserve API 设计文档

> 版本：1.0 | 日期：2026-06-29 | 基于需求规格文档 v1.0 和数据模型设计

---

## 目录

1. [通用约定](#一通用约定)
2. [认证模块](#二认证模块-auth)
3. [用户管理](#三用户管理-users)
4. [实验室管理](#四实验室管理-labs)
5. [开放时间](#五开放时间-labhours)
6. [预约管理](#六预约管理-bookings)
7. [设备管理](#七设备管理-equipment)
8. [设备借用](#八设备借用-borrows)
9. [课程管理](#九课程管理-courses)
10. [通知公告](#十通知公告-notices)
11. [实验室评价](#十一实验室评价-reviews)
12. [使用记录](#十二使用记录-usage-records)
13. [数据统计](#十三数据统计-dashboard)
14. [学生信息](#十四学生信息-students)
15. [报修记录](#十五报修记录-repair-logs)
16. [错误码参考](#十六错误码参考)

---

## 一、通用约定

### 1.1 Base URL

```
http://localhost:8080/api
```

### 1.2 认证

- 除 `/auth/register`、`/auth/login`、`/health` 外，所有接口需携带 JWT
- Header：`Authorization: Bearer <token>`
- Token 有效期：24 小时

### 1.3 统一响应格式

**成功响应：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { ... }
}
```

**错误响应：**

```json
{
  "code": "ERROR_CODE",
  "message": "人类可读的错误描述",
  "details": { "field": "具体字段错误信息" }
}
```

### 1.4 分页（Cursor-based Pagination）

**请求参数：**

| 参数      | 类型    | 必填 | 说明                              |
| --------- | ------- | ---- | --------------------------------- |
| cursor    | string  | 否   | 上一页最后一条记录的 ID，首次不传 |
| limit     | integer | 否   | 每页条数，默认 20，最大 100       |
| sortBy    | string  | 否   | 排序字段                          |
| sortOrder | string  | 否   | `asc` / `desc`                    |

**响应格式：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "records": [...],
    "count": 20,
    "hasMore": true,
    "nextCursor": "123",
    "total": 156
  }
}
```

### 1.5 HTTP 方法语义

| 方法   | 语义                    |
| ------ | ----------------------- |
| GET    | 查询（列表 / 详情）     |
| POST   | 创建                    |
| PUT    | 全量更新 / 状态变更操作 |
| DELETE | 软删除                  |

> 注：审批、取消、确认完成等操作用 `PUT` + 子资源路径（如 `/bookings/{id}/approve`），语义更明确。

### 1.6 角色权限矩阵

| 接口组                               | STUDENT | TEACHER        | ADMIN |
| ------------------------------------ | ------- | -------------- | ----- |
| 认证（注册/登录/个人信息/改密）      | 全部    | 全部           | 全部  |
| 用户管理（列表/创建/更新/禁用/删除） | —       | —              | 全部  |
| 实验室（列表/详情）                  | 查阅    | 查阅           | 全部  |
| 实验室（CRUD/状态/开放时间）         | —       | —              | 全部  |
| 预约（提交/我的预约/取消/修改）      | 自己的  | 自己的         | 全部  |
| 预约（审批/待审批列表）              | —       | 全部           | 全部  |
| 设备（列表/详情）                    | 查阅    | 查阅           | 全部  |
| 设备（CRUD）                         | —       | —              | 全部  |
| 借用（申请/我的借用）                | 自己的  | 自己的         | 全部  |
| 借用（审批/归还确认）                | —       | 全部           | 全部  |
| 课程（列表/我的课表）                | 查阅    | 查阅           | 全部  |
| 课程（创建/修改/删除）               | —       | 全部（自己的） | 全部  |
| 通知（列表/详情）                    | 查阅    | 查阅           | 全部  |
| 通知（发布/更新/删除）               | —       | 全部           | 全部  |
| 评价（列表/我的评价）                | 查阅    | 查阅           | 查阅  |
| 评价（提交/删除）                    | 自己的  | —              | 全部  |
| 使用记录                             | —       | 查阅           | 全部  |
| 仪表盘                               | —       | 查阅           | 全部  |
| 学生信息                             | —       | 查阅           | 全部  |

---

## 二、认证模块 (Auth)

### POST /api/auth/register — 用户注册

> F-01，默认注册为 STUDENT 角色

**Request Body：**

```json
{
  "username": "2021001",
  "password": "abc123",
  "realName": "张三",
  "email": "zhangsan@univ.edu.cn",
  "phone": "13800001111"
}
```

**Response 201：**

```json
{
  "code": "SUCCESS",
  "message": "注册成功",
  "data": null
}
```

**可能的错误：** `USERNAME_EXISTS`、`VALIDATION_ERROR`

---

### POST /api/auth/login — 用户登录

> F-02，JWT 认证，返回 token（24h）和用户信息

**Request Body：**

```json
{
  "username": "2021001",
  "password": "abc123"
}
```

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400,
    "user": {
      "id": "1",
      "username": "2021001",
      "realName": "张三",
      "role": "STUDENT",
      "avatar": null,
      "email": "zhangsan@univ.edu.cn",
      "phone": "13800001111",
      "enabled": true
    }
  }
}
```

**可能的错误：** `INVALID_CREDENTIALS`、`ACCOUNT_DISABLED`

---

### GET /api/auth/me — 获取当前用户信息

> F-03

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": "1",
    "username": "2021001",
    "realName": "张三",
    "role": "STUDENT",
    "avatar": null,
    "email": null,
    "phone": null,
    "enabled": true
  }
}
```

---

### PUT /api/auth/change-password — 修改密码

> F-04，需验证原密码

**Request Body：**

```json
{
  "oldPassword": "abc123",
  "newPassword": "def456"
}
```

**可能的错误：** `WRONG_PASSWORD`

---

## 三、用户管理 (Users)

> 所有接口 ADMIN 专属

### GET /api/users — 用户列表

> F-06

| 参数    | 类型    | 说明                                  |
| ------- | ------- | ------------------------------------- |
| role    | string  | 按角色筛选：STUDENT / TEACHER / ADMIN |
| enabled | boolean | 按启用状态筛选                        |
| cursor  | string  | 分页游标                              |
| limit   | integer | 每页条数                              |

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": "1",
        "username": "2021001",
        "realName": "张三",
        "email": null,
        "phone": null,
        "role": "STUDENT",
        "avatar": null,
        "enabled": true,
        "createdAt": "2026-06-29T00:00:00",
        "updatedAt": "2026-06-29T00:00:00"
      }
    ],
    "count": 1,
    "hasMore": false,
    "nextCursor": null,
    "total": 1
  }
}
```

---

### POST /api/users — 创建用户

> F-06，可指定任意角色

**Request Body：**

```json
{
  "username": "T001",
  "password": "teacher123",
  "realName": "李教授",
  "role": "TEACHER",
  "email": "lijs@univ.edu.cn",
  "phone": "13900002222"
}
```

**可能的错误：** `USERNAME_EXISTS`

---

### GET /api/users/{userId} — 用户详情

### PUT /api/users/{userId} — 更新用户

```json
{
  "realName": "张三丰",
  "email": "zsf@univ.edu.cn",
  "role": "STUDENT",
  "enabled": true
}
```

### DELETE /api/users/{userId} — 删除用户（软删除）

### PUT /api/users/{userId}/toggle-enabled — 启用/禁用

---

## 四、实验室管理 (Labs)

### GET /api/labs — 实验室列表

> F-07，所有角色可访问

| 参数   | 类型    | 说明                             |
| ------ | ------- | -------------------------------- |
| name   | string  | 按名称模糊搜索                   |
| status | string  | AVAILABLE / MAINTENANCE / CLOSED |
| cursor | string  | 分页游标                         |
| limit  | integer | 每页条数                         |

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": "1",
        "name": "计算机组成原理实验室",
        "location": "教学楼A-301",
        "capacity": 40,
        "description": "配备 40 台 FPGA 开发板",
        "imageUrl": null,
        "equipmentNum": 45,
        "status": "AVAILABLE",
        "managerId": "3",
        "managerName": "王管理员",
        "createdAt": "2026-06-29T00:00:00",
        "updatedAt": "2026-06-29T00:00:00"
      }
    ],
    "count": 1,
    "hasMore": false,
    "nextCursor": null,
    "total": 1
  }
}
```

---

### POST /api/labs — 创建实验室

> F-09，ADMIN 专属

```json
{
  "name": "计算机网络实验室",
  "location": "教学楼B-201",
  "capacity": 48,
  "description": "Cisco 网络设备实验",
  "imageUrl": null,
  "managerId": "3"
}
```

---

### GET /api/labs/{labId} — 实验室详情

> F-08，含开放时间、设备清单、评价列表

### PUT /api/labs/{labId} — 更新实验室

> F-09

```json
{
  "name": "计算机组成原理实验室（升级）",
  "capacity": 50,
  "status": "MAINTENANCE"
}
```

### DELETE /api/labs/{labId} — 删除实验室（软删除）

> F-09

### PUT /api/labs/{labId}/status — 更新实验室状态

> F-11

```json
{ "status": "AVAILABLE" }
```

---

## 五、开放时间 (LabHours)

### GET /api/labs/{labId}/hours — 查询开放时间

> F-10

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": [
    { "id": "1", "labId": "1", "dayOfWeek": 1, "openTime": "08:00", "closeTime": "12:00" },
    { "id": "2", "labId": "1", "dayOfWeek": 1, "openTime": "14:00", "closeTime": "18:00" },
    { "id": "3", "labId": "1", "dayOfWeek": 2, "openTime": "08:00", "closeTime": "18:00" }
  ]
}
```

### PUT /api/labs/{labId}/hours — 批量设置开放时间

> F-10，ADMIN 专属，全量替换

```json
{
  "hours": [
    { "dayOfWeek": 1, "openTime": "08:00", "closeTime": "12:00" },
    { "dayOfWeek": 1, "openTime": "14:00", "closeTime": "21:00" },
    { "dayOfWeek": 2, "openTime": "08:00", "closeTime": "21:00" }
  ]
}
```

---

## 六、预约管理 (Bookings)

> **核心模块**，状态机：`PENDING → APPROVED → COMPLETED`，`PENDING → REJECTED`，`PENDING/APPROVED → CANCELLED`

### GET /api/bookings — 预约列表

> F-15 / F-16，学生看到自己的预约，教师/管理员看到待审批列表

| 参数     | 类型    | 说明                    |
| -------- | ------- | ----------------------- |
| status   | string  | 按状态筛选              |
| labId    | string  | 按实验室筛选            |
| userId   | string  | 教师/管理员按申请人筛选 |
| dateFrom | string  | 日期范围起 (yyyy-MM-dd) |
| dateTo   | string  | 日期范围止 (yyyy-MM-dd) |
| cursor   | string  | 分页游标                |
| limit    | integer | 每页条数                |

---

### POST /api/bookings — 提交预约申请

> F-12

**Request Body：**

```json
{
  "labId": "1",
  "date": "2026-07-01",
  "startTime": "14:00",
  "endTime": "16:00",
  "purpose": "FPGA 课程设计实验",
  "personCount": 3
}
```

**可能的错误：** `TIME_CONFLICT`、`LAB_CLOSED`、`OUTSIDE_OPEN_HOURS`

---

### GET /api/bookings/available-slots — 查询可用时间段

> F-13，给定实验室和日期，返回时段可用性

| 参数  | 类型   | 必填 | 说明            |
| ----- | ------ | ---- | --------------- |
| labId | string | 是   | 实验室 ID       |
| date  | string | 是   | 日期 yyyy-MM-dd |

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": [
    { "startTime": "08:00", "endTime": "09:00", "available": true },
    { "startTime": "09:00", "endTime": "10:00", "available": true },
    { "startTime": "10:00", "endTime": "11:00", "available": false },
    { "startTime": "11:00", "endTime": "12:00", "available": true }
  ]
}
```

---

### GET /api/bookings/mine — 我的预约

> F-15，便捷接口

### GET /api/bookings/pending — 待审批列表

> F-16，教师/管理员

### GET /api/bookings/{bookingId} — 预约详情

### PUT /api/bookings/{bookingId} — 修改预约

> F-18，仅 PENDING 状态可修改

### PUT /api/bookings/{bookingId}/approve — 审批预约

> F-14，自动检测时间冲突（已审批预约 + 课程安排）

```json
{
  "approved": true
}
```

或驳回：

```json
{
  "approved": false,
  "rejectReason": "该时段已安排实验课程"
}
```

**可能的错误：** `TIME_CONFLICT`、`ALREADY_PROCESSED`

---

### PUT /api/bookings/{bookingId}/cancel — 取消预约

> F-17，PENDING 或 APPROVED 状态可取消

### PUT /api/bookings/{bookingId}/complete — 确认完成

> F-19，标记为 COMPLETED，自动生成使用记录

---

## 七、设备管理 (Equipment)

### GET /api/equipment — 设备列表

> F-21

| 参数   | 类型   | 说明                               |
| ------ | ------ | ---------------------------------- |
| labId  | string | 按实验室筛选                       |
| status | string | AVAILABLE / BORROWED / MAINTENANCE |
| name   | string | 按名称模糊搜索                     |

### POST /api/equipment — 登记设备

> F-22，ADMIN 专属

```json
{
  "labId": "1",
  "name": "FPGA 开发板",
  "model": "Xilinx Artix-7",
  "serialNumber": "FPGA-2026-001",
  "description": "第 3 批次采购"
}
```

**可能的错误：** `SERIAL_NUMBER_EXISTS`

### GET /api/equipment/{equipmentId} — 设备详情

### PUT /api/equipment/{equipmentId} — 更新设备

> F-22

### DELETE /api/equipment/{equipmentId} — 删除设备（软删除）

### PUT /api/equipment/{equipmentId}/status — 更新设备状态

> F-23

```json
{ "status": "MAINTENANCE" }
```

---

## 八、设备借用 (Borrows)

> 状态机：`PENDING → APPROVED → BORROWING → RETURNED`，`PENDING → REJECTED`

### GET /api/borrows — 借用列表

> F-27

| 参数        | 类型   | 说明                                                 |
| ----------- | ------ | ---------------------------------------------------- |
| status      | string | PENDING / APPROVED / REJECTED / BORROWING / RETURNED |
| equipmentId | string | 按设备筛选                                           |
| userId      | string | 按用户筛选                                           |

### POST /api/borrows — 借用申请

> F-24

```json
{
  "equipmentId": "1",
  "borrowDate": "2026-07-01",
  "expectedReturn": "2026-07-05",
  "purpose": "课程设计需要"
}
```

**可能的错误：** `EQUIPMENT_UNAVAILABLE`

### GET /api/borrows/mine — 我的借用记录

> F-27

### GET /api/borrows/{borrowId} — 借用详情

### PUT /api/borrows/{borrowId}/approve — 审批借用

> F-25

### PUT /api/borrows/{borrowId}/return — 确认归还

> F-26，管理员确认，设备状态恢复为 AVAILABLE

```json
{ "actualReturn": "2026-07-04" }
```

---

## 九、课程管理 (Courses)

### GET /api/courses — 课程列表

> F-29

| 参数      | 类型   | 说明                   |
| --------- | ------ | ---------------------- |
| semester  | string | 学期，如 "2025-2026-1" |
| teacherId | string | 按任课教师筛选         |
| labId     | string | 按实验室筛选           |
| className | string | 按班级筛选             |

### POST /api/courses — 创建课程

> F-28，教师/管理员

```json
{
  "name": "数字逻辑实验",
  "labId": "1",
  "teacherId": "2",
  "semester": "2025-2026-1",
  "dayOfWeek": 3,
  "startTime": "08:00",
  "endTime": "10:00",
  "startDate": "2026-03-01",
  "endDate": "2026-07-15",
  "className": "计科2101"
}
```

### GET /api/courses/mine — 我的课表

> F-30，学生按班级、教师按任教

### GET /api/courses/{courseId} — 课程详情

### PUT /api/courses/{courseId} — 修改课程

> F-31

### DELETE /api/courses/{courseId} — 删除课程

> F-31

---

## 十、通知公告 (Notices)

### GET /api/notices — 通知列表

> F-33

| 参数     | 类型   | 说明                         |
| -------- | ------ | ---------------------------- |
| type     | string | GENERAL / LAB / EQUIPMENT    |
| priority | string | LOW / NORMAL / HIGH / URGENT |

### POST /api/notices — 发布通知

> F-32，教师/管理员

```json
{
  "title": "实验室维护通知",
  "content": "教学楼A-301 实验室将于本周六进行设备维护，届时暂停开放。",
  "type": "LAB",
  "priority": "HIGH",
  "labId": "1"
}
```

### GET /api/notices/{noticeId} — 通知详情

> F-34

### PUT /api/notices/{noticeId} — 更新通知

### DELETE /api/notices/{noticeId} — 删除通知

---

## 十一、实验室评价 (Reviews)

### GET /api/reviews — 评价列表

> F-36

| 参数   | 类型   | 说明                     |
| ------ | ------ | ------------------------ |
| labId  | string | 按实验室筛选             |
| userId | string | 按用户筛选（"我的评价"） |

### POST /api/reviews — 提交评价

> F-35，学生完成预约后评分（1-5 星）

```json
{
  "bookingId": "42",
  "rating": 4,
  "comment": "设备运行良好，环境整洁"
}
```

**可能的错误：** `BOOKING_NOT_COMPLETED`、`ALREADY_REVIEWED`

### GET /api/reviews/mine — 我的评价

> F-37

### DELETE /api/reviews/{reviewId} — 删除评价

---

## 十二、使用记录 (Usage Records)

### GET /api/usage-records — 使用记录查询

> F-38，教师/管理员

| 参数     | 类型   | 说明         |
| -------- | ------ | ------------ |
| labId    | string | 按实验室筛选 |
| userId   | string | 按用户筛选   |
| dateFrom | string | 日期范围起   |
| dateTo   | string | 日期范围止   |

### POST /api/usage-records/export — 导出 CSV

> F-39，ADMIN 专属

```json
{
  "resource": "usageRecords",
  "format": "csv",
  "filters": { "dateFrom": "2026-01-01", "dateTo": "2026-06-30" }
}
```

Response: `Content-Type: text/csv`

---

## 十三、数据统计 (Dashboard)

### GET /api/dashboard/kpi — 综合概览

> F-40

**Response 200：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "todayBookings": 12,
    "todayBorrows": 3,
    "labUsageRate": 0.65,
    "pendingApprovals": 5
  }
}
```

---

### GET /api/dashboard/lab-usage — 实验室使用率统计

> F-41

| 参数     | 类型   | 说明         |
| -------- | ------ | ------------ |
| dateFrom | string | 统计起始日期 |
| dateTo   | string | 统计结束日期 |

### GET /api/dashboard/equipment-usage — 设备利用率统计

> F-42

### GET /api/dashboard/student-ranking — 学生使用排行

> F-43

| 参数  | 类型    | 说明             |
| ----- | ------- | ---------------- |
| limit | integer | 前 N 名，默认 10 |

---

## 十四、学生信息 (Students)

### GET /api/students — 学生列表

| 参数  | 类型   | 说明         |
| ----- | ------ | ------------ |
| labId | string | 按实验室筛选 |
| name  | string | 按姓名搜索   |

### POST /api/students — 录入学生信息

```json
{
  "labId": "1",
  "name": "张三",
  "gender": "MALE",
  "age": 20,
  "address": "计科2101"
}
```

### GET /api/students/{studentId} — 学生详情

### PUT /api/students/{studentId} — 更新学生信息

### DELETE /api/students/{studentId} — 删除学生信息

---

## 十五、报修记录 (Repair Logs)

### GET /api/repair-logs — 报修列表

| 参数        | 类型   | 说明                              |
| ----------- | ------ | --------------------------------- |
| equipmentId | string | 按设备筛选                        |
| status      | string | PENDING / IN_PROGRESS / COMPLETED |

### POST /api/repair-logs — 提交报修

```json
{
  "equipmentId": "1",
  "description": "FPGA 开发板无法上电，电源指示灯不亮"
}
```

### GET /api/repair-logs/{repairLogId} — 报修详情

### PUT /api/repair-logs/{repairLogId}/status — 更新报修状态

```json
{ "status": "IN_PROGRESS" }
```

---

## 十六、错误码参考

### 通用错误码

| 错误码             | HTTP 状态 | 说明                |
| ------------------ | --------- | ------------------- |
| SUCCESS            | 200/201   | 操作成功            |
| VALIDATION_ERROR   | 400       | 请求参数校验失败    |
| UNAUTHORIZED       | 401       | 未认证或 token 过期 |
| FORBIDDEN          | 403       | 无权限              |
| NOT_FOUND          | 404       | 资源不存在          |
| METHOD_NOT_ALLOWED | 405       | 不支持的 HTTP 方法  |
| INTERNAL_ERROR     | 500       | 服务器内部错误      |

### 业务错误码

| 错误码                  | HTTP 状态 | 说明                         |
| ----------------------- | --------- | ---------------------------- |
| USERNAME_EXISTS         | 409       | 用户名已存在                 |
| SERIAL_NUMBER_EXISTS    | 409       | 设备序列号已存在             |
| INVALID_CREDENTIALS     | 401       | 用户名或密码错误             |
| ACCOUNT_DISABLED        | 403       | 账号已被禁用                 |
| WRONG_PASSWORD          | 400       | 原密码错误                   |
| TIME_CONFLICT           | 409       | 时间段冲突                   |
| LAB_CLOSED              | 400       | 实验室已关闭或维护中         |
| OUTSIDE_OPEN_HOURS      | 400       | 预约时间不在开放时间范围内   |
| EQUIPMENT_UNAVAILABLE   | 409       | 设备不可用（已借出或维修中） |
| ALREADY_PROCESSED       | 400       | 该记录已被审批过             |
| ALREADY_REVIEWED        | 409       | 该预约已评价过               |
| BOOKING_NOT_COMPLETED   | 400       | 预约未完成，不可评价         |
| BOOKING_NOT_CANCELLABLE | 400       | 当前状态不可取消             |
| BOOKING_NOT_EDITABLE    | 400       | 仅 PENDING 状态可修改        |

---

## 附录 A：接口索引

| 方法   | 路径                                  | 功能编号  | 角色          |
| ------ | ------------------------------------- | --------- | ------------- |
| GET    | /api/health                           | —         | 无限制        |
| POST   | /api/auth/register                    | F-01      | 无限制        |
| POST   | /api/auth/login                       | F-02      | 无限制        |
| GET    | /api/auth/me                          | F-03      | 已认证        |
| PUT    | /api/auth/change-password             | F-04      | 已认证        |
| GET    | /api/users                            | F-06      | ADMIN         |
| POST   | /api/users                            | F-06      | ADMIN         |
| GET    | /api/users/{userId}                   | F-06      | ADMIN         |
| PUT    | /api/users/{userId}                   | F-06      | ADMIN         |
| DELETE | /api/users/{userId}                   | F-06      | ADMIN         |
| PUT    | /api/users/{userId}/toggle-enabled    | F-06      | ADMIN         |
| GET    | /api/labs                             | F-07      | 已认证        |
| POST   | /api/labs                             | F-09      | ADMIN         |
| GET    | /api/labs/{labId}                     | F-08      | 已认证        |
| PUT    | /api/labs/{labId}                     | F-09      | ADMIN         |
| DELETE | /api/labs/{labId}                     | F-09      | ADMIN         |
| PUT    | /api/labs/{labId}/status              | F-11      | ADMIN         |
| GET    | /api/labs/{labId}/hours               | F-10      | 已认证        |
| PUT    | /api/labs/{labId}/hours               | F-10      | ADMIN         |
| GET    | /api/bookings                         | F-15/F-16 | 已认证        |
| POST   | /api/bookings                         | F-12      | 已认证        |
| GET    | /api/bookings/available-slots         | F-13      | 已认证        |
| GET    | /api/bookings/mine                    | F-15      | 已认证        |
| GET    | /api/bookings/pending                 | F-16      | TEACHER/ADMIN |
| GET    | /api/bookings/{bookingId}             | —         | 已认证        |
| PUT    | /api/bookings/{bookingId}             | F-18      | 申请人        |
| PUT    | /api/bookings/{bookingId}/approve     | F-14      | TEACHER/ADMIN |
| PUT    | /api/bookings/{bookingId}/cancel      | F-17      | 申请人        |
| PUT    | /api/bookings/{bookingId}/complete    | F-19      | TEACHER/ADMIN |
| GET    | /api/equipment                        | F-21      | 已认证        |
| POST   | /api/equipment                        | F-22      | ADMIN         |
| GET    | /api/equipment/{equipmentId}          | —         | 已认证        |
| PUT    | /api/equipment/{equipmentId}          | F-22      | ADMIN         |
| DELETE | /api/equipment/{equipmentId}          | F-22      | ADMIN         |
| PUT    | /api/equipment/{equipmentId}/status   | F-23      | ADMIN         |
| GET    | /api/borrows                          | —         | 已认证        |
| POST   | /api/borrows                          | F-24      | 已认证        |
| GET    | /api/borrows/mine                     | F-27      | 已认证        |
| GET    | /api/borrows/{borrowId}               | —         | 已认证        |
| PUT    | /api/borrows/{borrowId}/approve       | F-25      | TEACHER/ADMIN |
| PUT    | /api/borrows/{borrowId}/return        | F-26      | ADMIN         |
| GET    | /api/courses                          | F-29      | 已认证        |
| POST   | /api/courses                          | F-28      | TEACHER/ADMIN |
| GET    | /api/courses/mine                     | F-30      | 已认证        |
| GET    | /api/courses/{courseId}               | —         | 已认证        |
| PUT    | /api/courses/{courseId}               | F-31      | TEACHER/ADMIN |
| DELETE | /api/courses/{courseId}               | F-31      | TEACHER/ADMIN |
| GET    | /api/notices                          | F-33      | 已认证        |
| POST   | /api/notices                          | F-32      | TEACHER/ADMIN |
| GET    | /api/notices/{noticeId}               | F-34      | 已认证        |
| PUT    | /api/notices/{noticeId}               | —         | TEACHER/ADMIN |
| DELETE | /api/notices/{noticeId}               | —         | TEACHER/ADMIN |
| GET    | /api/reviews                          | F-36      | 已认证        |
| POST   | /api/reviews                          | F-35      | 已认证        |
| GET    | /api/reviews/mine                     | F-37      | 已认证        |
| DELETE | /api/reviews/{reviewId}               | —         | 本人/ADMIN    |
| GET    | /api/usage-records                    | F-38      | TEACHER/ADMIN |
| POST   | /api/usage-records/export             | F-39      | ADMIN         |
| GET    | /api/dashboard/kpi                    | F-40      | TEACHER/ADMIN |
| GET    | /api/dashboard/lab-usage              | F-41      | TEACHER/ADMIN |
| GET    | /api/dashboard/equipment-usage        | F-42      | TEACHER/ADMIN |
| GET    | /api/dashboard/student-ranking        | F-43      | TEACHER/ADMIN |
| GET    | /api/students                         | —         | 已认证        |
| POST   | /api/students                         | —         | TEACHER/ADMIN |
| GET    | /api/students/{studentId}             | —         | 已认证        |
| PUT    | /api/students/{studentId}             | —         | TEACHER/ADMIN |
| DELETE | /api/students/{studentId}             | —         | ADMIN         |
| GET    | /api/repair-logs                      | —         | 已认证        |
| POST   | /api/repair-logs                      | —         | 已认证        |
| GET    | /api/repair-logs/{repairLogId}        | —         | 已认证        |
| PUT    | /api/repair-logs/{repairLogId}/status | —         | ADMIN         |

> **总计：67 个接口**，覆盖 14 类资源，43 项功能需求
