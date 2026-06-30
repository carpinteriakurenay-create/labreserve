# M2 Bug 修复记录

> 日期：2026-06-30

## Bug #1: LabHours 全量替换 500 — 软删除 + UNIQUE KEY 冲突

## 现象

```
PUT /api/labs/1/hours → 500 Internal Server Error
```

先 `GET /api/labs/1/hours` 返回空数组，`PUT` 设置第一批数据成功（200），第二次 `PUT` 替换为不同内容时报 500。从第一次成功后的 GET 看，只保留了第一批的部分数据，第二次批量写入未完成。

## 根因分析

`batchReplaceLabHours` 的逻辑是"先删旧数据，再 insert 新数据"。删除调用 `labHoursMapper.delete(wrapper)`，MyBatis-Plus 的 `@TableLogic` 将其转换为软删除：

```sql
UPDATE lab_hours SET deleted = 1 WHERE lab_id = ?
```

旧行物理上仍然存在。而 `lab_hours` 表有唯一约束：

```sql
UNIQUE KEY uk_lab_hours_day_open (lab_id, day_of_week, open_time)
```

当新数据集合包含与已软删除行相同的 `(lab_id, day_of_week, open_time)` 时，INSERT 违反唯一约束，抛出 `DuplicateKeyException`。

| 步骤 | 操作                                              | SQL                                 | 结果                     |
| ---- | ------------------------------------------------- | ----------------------------------- | ------------------------ |
| 1    | 第一次 PUT `[{D1,08:00-12:00}, {D1,14:00-18:00}]` | DELETE 软删 → INSERT 2 行           | OK                       |
| 2    | 第二次 PUT `[{D2,08:00-21:00}]`                   | DELETE 软删 → INSERT 1 行           | OK                       |
| 3    | 第三次 PUT `[{D1,08:00-12:00}]`                   | DELETE 软删 → INSERT `(1,D1,08:00)` | ❌ DuplicateKeyException |

### 根本原因总结

`@TableLogic` 软删除与 UNIQUE KEY 组合在「先删后插」的全量替换模式下有天然冲突 — 软删除保留行，唯一索引仍对所有行（含 deleted=1）生效。

## 修复方案

### 修复 1：LabHoursMapper 新增硬删除方法

**文件**：`apps/api/src/main/java/com/labreserve/mapper/LabHoursMapper.java`

用 `@Delete` 注解写原生 SQL，绕过 `@TableLogic`：

```java
@Delete("DELETE FROM lab_hours WHERE lab_id = #{labId}")
void hardDeleteByLabId(@Param("labId") Long labId);
```

### 修复 2：LabService 改用硬删除

**文件**：`apps/api/src/main/java/com/labreserve/service/LabService.java`

```java
// 修改前
labHoursMapper.delete(new LambdaQueryWrapper<LabHours>().eq(LabHours::getLabId, labId));

// 修改后
labHoursMapper.hardDeleteByLabId(labId);
```

## 验证

```bash
# 第一次设置
curl -s -X PUT localhost:8080/api/labs/1/hours \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"hours":[{"dayOfWeek":1,"openTime":"08:00","closeTime":"12:00"},{"dayOfWeek":1,"openTime":"14:00","closeTime":"18:00"}]}'
# → 200

# 第二次替换（包含与第一次相同的 key）
curl -s -X PUT localhost:8080/api/labs/1/hours \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"hours":[{"dayOfWeek":1,"openTime":"08:00","closeTime":"12:00"},{"dayOfWeek":2,"openTime":"08:00","closeTime":"21:00"}]}'
# → 200

# 验证全量替换
curl -s -X GET localhost:8080/api/labs/1/hours \
  -H "Authorization: Bearer $TOKEN"
# → 200 仅 2 条数据，旧数据已被物理删除
```

## 经验教训

1. `@TableLogic` 软删除与 UNIQUE KEY 组合时，`BaseMapper.delete(wrapper)` 只标记 deleted=1，不释放唯一键
2. 全量替换语义下应使用物理删除（`DELETE FROM`），而非软删除（`UPDATE SET deleted=1`）
3. 需要物理删除时必须写原生 SQL 或 XML mapper，`BaseMapper` 的 delete 方法行为受 `@TableLogic` 控制

---

## Bug #2: 冲突检测漏检 PENDING 状态预约 — 只检查 APPROVED 预约导致同时间多笔 PENDING

> 日期：2026-06-30 | 影响范围：M2-9 `POST /api/bookings`、`PUT /api/bookings/{id}/approve`、`GET /api/bookings/available-slots`

## 现象

```
POST /api/bookings (09:00-11:00) → 201 创建成功
POST /api/bookings (09:00-11:00) → 201 创建成功（同实验室同日期同时段）
GET /api/bookings/available-slots → 时段仍标记为 available:true
```

同一实验室同一日期同一时段提交了多笔 PENDING 预约，冲突检测未触发。

## 根因分析

`BookingService` 中三处冲突检测查询只过滤 `BookingStatus.APPROVED`：

```java
// createBooking — 第 72 行
.eq(Booking::getStatus, BookingStatus.APPROVED)

// approveBooking — 第 245 行
.eq(Booking::getStatus, BookingStatus.APPROVED)

// getAvailableSlots — 第 347 行
.eq(Booking::getStatus, BookingStatus.APPROVED)
```

PENDING 状态的预约不参与冲突检测，导致同一时段可以堆积多笔 PENDING 记录。

| 步骤 | 操作                              | 已有预约              | 预期              | 实际                    |
| ---- | --------------------------------- | --------------------- | ----------------- | ----------------------- |
| 1    | POST booking A (08:00-10:00)      | 无                    | 201               | 201                     |
| 2    | POST booking B (09:00-11:00)      | A=PENDING 08:00-10:00 | 409 TIME_CONFLICT | 201（PENDING 未被检测） |
| 3    | POST booking C (09:00-11:00)      | A+B 均为 PENDING      | 409 TIME_CONFLICT | 201                     |
| 4    | GET available-slots (08:00-12:00) | 3 笔 PENDING          | available:false   | available:true          |

### 根本原因总结

冲突检测范围过窄。`PENDING` 预约已占用时段，应视为冲突源。`CANCELLED` 和 `REJECTED` 不占用时段，`COMPLETED` 是已结束的预约（只占历史时段，不影响新预约）。

## 修复方案

### 修复 1：createBooking 冲突检测扩展为 PENDING + APPROVED

**文件**：`apps/api/src/main/java/com/labreserve/service/BookingService.java`

```java
// 修改前
List<Booking> approvedBookings = bookingMapper.selectList(
        new LambdaQueryWrapper<Booking>()
                .eq(Booking::getLabId, request.getLabId())
                .eq(Booking::getDate, date)
                .eq(Booking::getStatus, BookingStatus.APPROVED));

// 修改后
List<Booking> conflictingBookings = bookingMapper.selectList(
        new LambdaQueryWrapper<Booking>()
                .eq(Booking::getLabId, request.getLabId())
                .eq(Booking::getDate, date)
                .and(w -> w.eq(Booking::getStatus, BookingStatus.PENDING)
                        .or()
                        .eq(Booking::getStatus, BookingStatus.APPROVED)));
```

### 修复 2：approveBooking 冲突重检同步修改

```java
// 修改前
.in(Booking::getStatus, BookingStatus.PENDING, BookingStatus.APPROVED) // 原为只用 APPROVED

// 修改后：使用 .and(w -> w.eq(PENDING).or().eq(APPROVED))
```

### 修复 3：getAvailableSlots 冲突检测同步修改

同样改为 `conflictingBookings` 包含 PENDING + APPROVED。

> **注**：三处修改使用 `replace_all: true` 批量完成。MySQL 不支持 `BookingStatus` 枚举类型，故 `.in()` varargs 可能不兼容，改用 `.and(w -> .eq().or().eq())` 方式。

## 验证

```bash
# 1. 创建第一笔预约
curl -s -X POST localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"labId":1,"date":"2026-07-01","startTime":"08:00","endTime":"10:00","purpose":"实验","personCount":2}'
# → 201  {"code":"SUCCESS","message":"预约提交成功",...}

# 2. 冲突时段预约 → 应返回 409
curl -s -X POST localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"labId":1,"date":"2026-07-01","startTime":"09:00","endTime":"11:00","purpose":"冲突","personCount":1}'
# → 409 {"code":"TIME_CONFLICT","message":"预约时间与已有安排冲突",...}

# 3. 可用时段应排除已被 PENDING 预约占用的格子
curl -s -X GET "localhost:8080/api/bookings/available-slots?labId=1&date=2026-07-01" \
  -H "Authorization: Bearer $TOKEN"
# → 08:00-09:00 available:false / 09:00-10:00 available:false
```

## 经验教训

1. 冲突检测应覆盖所有占用时段的预约状态（PENDING + APPROVED），而非仅 APPROVED
2. 状态机语义决定冲突范围：PENDING/APPROVED 占用资源，REJECTED/CANCELLED/COMPLETED 不占用
3. 同一逻辑出现在多处（create/approve/available-slots）时必须全部同步修改，否则不一致
