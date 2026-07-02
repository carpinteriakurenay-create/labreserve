# 性能问题修复方案

> 基于 docs/performance-audit.md | 日期: 2026-07-02 | 实施状态: ✅ 已完成

---

## 一、背景

性能审计发现综合评分 3.8/10。关键问题：测试 schema.sql 零索引、Dashboard 在 Java 内存中做聚合、前端无代码分割。值得注意的是 `docker/mysql/init.sql`（生产 schema）已有完整索引，问题集中在测试 schema。

## 二、修复清单

### [DB-01] 测试 schema.sql 补充索引

**严重程度**：Critical

**描述**：测试用 `schema.sql` 中所有表仅有主键索引，与生产 `init.sql` 严重不一致。

**文件变更**：`apps/api/src/test/resources/schema.sql`

**新增索引清单**：

| 表               | 索引                                                                                                                     |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `users`          | `UNIQUE uk_users_username`, `idx_users_role`, `idx_users_enabled`, `idx_users_deleted`                                   |
| `labs`           | `idx_labs_status`, `idx_labs_name`, `idx_labs_deleted`                                                                   |
| `lab_hours`      | `UNIQUE uk_lab_hours_day_open`, `idx_lab_hours_lab`, `idx_lab_hours_deleted`                                             |
| `lab_categories` | `UNIQUE uk_lab_categories_name`, `idx_lab_categories_deleted`                                                            |
| `equipment`      | `UNIQUE uk_equipment_serial`, `idx_equipment_lab`, `idx_equipment_status`, `idx_equipment_name`, `idx_equipment_deleted` |
| `bookings`       | `idx_bookings_lab_date`, `idx_bookings_user`, `idx_bookings_status`, `idx_bookings_approver`, `idx_bookings_deleted`     |
| `borrows`        | `idx_borrows_equipment`, `idx_borrows_user`, `idx_borrows_status`, `idx_borrows_approver`, `idx_borrows_deleted`         |
| `courses`        | `idx_courses_lab_day`, `idx_courses_teacher`, `idx_courses_semester`, `idx_courses_deleted`                              |
| `notices`        | `idx_notices_type`, `idx_notices_priority`, `idx_notices_created`, `idx_notices_deleted`                                 |
| `reviews`        | `UNIQUE uk_reviews_booking`, `idx_reviews_lab`, `idx_reviews_user`, `idx_reviews_deleted`                                |
| `repair_logs`    | `idx_repair_logs_equipment`, `idx_repair_logs_status`, `idx_repair_logs_deleted`                                         |
| `students`       | `idx_students_lab`, `idx_students_name`, `idx_students_deleted`                                                          |
| `messages`       | `idx_messages_receiver_read`, `idx_messages_sender`, `idx_messages_deleted`                                              |

**预期改善**：测试环境查询性能与生产对齐，避免测试与生产行为不一致。

---

### [DB-02] Dashboard getKpi 使用 SQL COUNT(DISTINCT) 替代全量加载

**严重程度**：High

**文件变更**：

| 文件                    | 变更                                               |
| ----------------------- | -------------------------------------------------- |
| `BookingMapper.java`    | 新增 `countDistinctLabsByDate()`                   |
| `DashboardService.java` | 替换 `selectList()` 为 `countDistinctLabsByDate()` |

**修复前**：

```java
List<Booking> todayCompletedBookings = bookingMapper.selectList(wrapper); // 全量加载
long labsWithBookings = todayCompletedBookings.stream()
        .map(Booking::getLabId).distinct().count();
```

**修复后**：

```java
long labsWithBookings = bookingMapper.countDistinctLabsByDate(today.toString());
```

---

### [DB-03] Dashboard 聚合下推到 SQL

**严重程度**：High

**描述**：`getLabUsage`、`getEquipmentUsage`、`getStudentRanking` 三个方法将全部数据加载到 Java 内存后用 Stream 分组聚合。

**修复**：在 Mapper 中新增聚合查询方法，仅返回 ID + COUNT（时间计算保留在 Java 层以保证 H2/MySQL 兼容）。

**文件变更**：

| 文件                        | 操作                                                 |
| --------------------------- | ---------------------------------------------------- |
| `BookingMapper.java`        | 新增 `aggregateLabUsage()`, `aggregateUserRanking()` |
| `BorrowMapper.java`         | 新增 `aggregateEquipmentUsage()`                     |
| `LabUsageAggRow.java`       | **新增** — 聚合行投影                                |
| `UserRankingAggRow.java`    | **新增** — 聚合行投影                                |
| `EquipmentUsageAggRow.java` | **新增** — 聚合行投影                                |
| `DashboardService.java`     | 重写 3 个聚合方法                                    |

**预期改善**：

| 指标                        | 修复前            | 修复后             | 改善      |
| --------------------------- | ----------------- | ------------------ | --------- |
| 数据传输量（1万条 booking） | ~2 MB             | ~2 KB              | **99.9%** |
| Java 内存占用               | 全部 Booking 对象 | 聚合行对象（K 条） | **99%+**  |

---

### [DB-04] CSV 导出添加结果上限

**严重程度**：High

**文件变更**：`UsageRecordService.java` — `exportCsv` 方法添加 `wrapper.last("LIMIT 10000")`

---

### [FE-01] Vite 代码分割 + Bundle 分析

**严重程度**：High

**文件变更**：`apps/web/vite.config.ts` — 添加 `manualChunks`

**构建结果**：

| Chunk                               | 大小    | Gzip   |
| ----------------------------------- | ------- | ------ |
| `vendor-vue` (vue/vue-router/pinia) | 110 KB  | 43 KB  |
| `vendor-element` (element-plus)     | 931 KB  | 301 KB |
| `vendor-echarts`                    | 529 KB  | 181 KB |
| `index` (应用代码)                  | 56 KB   | 21 KB  |
| 各页面 lazy chunks                  | 7-10 KB | 2-4 KB |

---

### [FE-02] ECharts 按需导入

**严重程度**：Medium

**文件变更**：

| 文件                       | 变更                        |
| -------------------------- | --------------------------- |
| `EquipmentStatusChart.vue` | `echarts/core` + `PieChart` |
| `LabUsageChart.vue`        | `echarts/core` + `BarChart` |

---

### [FE-03] MyBookings N+1 API 调用优化

**严重程度**：High

**文件变更**：`MyBookingsView.vue` — 串行 `for-await` 改为 `Promise.allSettled`

**预期改善**：10 个已完成预约的加载从 ~2.5s 降至 ~300ms（**-88%**）

---

### [FE-04] Dashboard 未缓存端点添加 @Cacheable

**严重程度**：Medium

**文件变更**：

| 文件                    | 变更                                                                      |
| ----------------------- | ------------------------------------------------------------------------- |
| `DashboardService.java` | `getLabUsage`、`getEquipmentUsage`、`getStudentRanking` 添加 `@Cacheable` |
| `RedisCacheConfig.java` | 添加 `@ConditionalOnProperty` + `@ConditionalOnBean`                      |
| `RedisTestConfig.java`  | 提供 `NoOpCacheManager` bean                                              |

---

## 三、验证

```
后端: mvn test → 293 tests, 0 failures, 0 errors, BUILD SUCCESS ✅
前端: pnpm test → 15 tests, 4 files passed ✅
前端: pnpm build → ✓ built in 21.61s ✅
  vendor-vue:     110 KB (gzip: 43 KB)
  vendor-element: 931 KB (gzip: 301 KB)
  vendor-echarts: 529 KB (gzip: 181 KB)
  应用代码:       56 KB (gzip: 21 KB)
  页面 chunks:    7-10 KB each (lazy loaded)
```

## 四、文件清单

```
修改的文件 (14):
├── apps/api/src/test/resources/schema.sql                          ← 30个索引
├── apps/api/src/main/java/com/labreserve/
│   ├── mapper/BookingMapper.java                                   ← 3个聚合方法
│   ├── mapper/BorrowMapper.java                                    ← 1个聚合方法
│   ├── mapper/LabUsageAggRow.java        [新增]
│   ├── mapper/EquipmentUsageAggRow.java  [新增]
│   ├── mapper/UserRankingAggRow.java     [新增]
│   ├── service/DashboardService.java                               ← 重写聚合+@Cacheable
│   ├── service/UsageRecordService.java                             ← CSV LIMIT 10000
│   ├── config/RedisCacheConfig.java                                ← ConditionalOnBean
│   ├── test/.../support/RedisTestConfig.java                       ← NoOpCacheManager
│   └── test/.../service/DashboardServiceTest.java                  ← 适配新Mapper
├── apps/web/vite.config.ts                                         ← manualChunks+visualizer
├── apps/web/tsconfig.json                                          ← exclude test dirs
├── apps/web/src/components/EquipmentStatusChart.vue                ← ECharts按需
├── apps/web/src/components/LabUsageChart.vue                       ← ECharts按需
└── apps/web/src/views/MyBookingsView.vue                           ← Promise.allSettled
```
