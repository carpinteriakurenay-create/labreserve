# M3 Bug 修复记录

> 日期：2026-07-01

## Bug #1: 重复序列号创建设备返回 500 而非 409

### 现象

序列号 `FPGA-2026-001` 首次创建成功，删除后再创建相同序列号设备时返回 500：

```
POST /api/equipment → 500 Internal Server Error
```

服务端日志：

```
java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'FPGA-2026-001' for key 'equipment.uk_equipment_serial'
```

### 根因

数据库 `uk_equipment_serial` 唯一索引覆盖**所有行**（包括 `deleted=1` 的软删除行），但 `EquipmentService.createEquipment()` 中：

```java
Long count = equipmentMapper.selectCount(
    new LambdaQueryWrapper<Equipment>().eq(Equipment::getSerialNumber, request.getSerialNumber())
);
```

`selectCount` 被 `@TableLogic` 自动追加 `AND deleted=0`，已软删除的记录查不到，`count=0` 认为无冲突 → INSERT 直接撞上唯一索引，抛 `DuplicateKeyException`。

| 层  | 根因                                                | 现象                                       |
| --- | --------------------------------------------------- | ------------------------------------------ |
| 1   | `@TableLogic` 过滤 `deleted=0` 导致查不出已删除记录 | `selectCount` 返回 0                       |
| 2   | 唯一索引不区分软删除                                | INSERT 抛 `DuplicateKeyException`          |
| 3   | Service 层未捕获该异常                              | 500（GlobalExceptionHandler 无此异常映射） |

### 修复方案

**文件**：`apps/api/src/main/java/com/labreserve/service/EquipmentService.java`

在 `createEquipment()` 的 `insert()` 调用和 `updateEquipment()` 的 `update()` 调用上添加 `DuplicateKeyException` 捕获，转为 `SERIAL_NUMBER_EXISTS` 业务异常：

```java
// createEquipment()
try {
    equipmentMapper.insert(equipment);
} catch (DuplicateKeyException e) {
    throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
}

// updateEquipment()
try {
    equipmentMapper.update(wrapper);
} catch (DuplicateKeyException e) {
    throw new BusinessException("SERIAL_NUMBER_EXISTS", "设备序列号已存在");
}
```

`SERIAL_NUMBER_EXISTS` 已在 `GlobalExceptionHandler` 中映射到 409 CONFLICT，无需额外修改。

### 验证

```bash
# 1. 首次创建成功
curl -s -X POST localhost:8080/api/equipment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"labId":"1","name":"FPGA 开发板","serialNumber":"FPGA-2026-001"}'
# → 201 {"code":"SUCCESS","message":"创建成功",...}

# 2. 删除（软删除）
curl -s -X DELETE localhost:8080/api/equipment/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# → 200

# 3. 再次创建相同序列号 → 应返回 409
curl -s -X POST localhost:8080/api/equipment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"labId":"1","name":"FPGA 开发板 v2","serialNumber":"FPGA-2026-001"}'
# → 409 {"code":"SERIAL_NUMBER_EXISTS","message":"设备序列号已存在","details":null}
```

### 经验教训

1. MyBatis-Plus 的 `@TableLogic` + `selectCount` 不是可靠的唯一性校验手段 —— 软删除的记录不会被查出，但数据库层的唯一约束仍然生效
2. 对唯一列做写入操作时，应始终在 Service 层 catch `DuplicateKeyException`，作为最后一道防线
3. 更好的方案是数据库层面用 **唯一索引 + NULL 列**（软删除时将 deleted 设为 ID 而非 1），或使用唯一索引包含 `deleted` 字段
