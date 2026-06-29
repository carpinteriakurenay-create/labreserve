# ADR-001: 选择 MyBatis-Plus 作为 ORM 框架

## 状态

已采纳

## 上下文

项目需要与 MySQL 数据库交互。需要选择 Java 持久层框架。

## 决策

使用 MyBatis-Plus 3.5。

## 理由

- 团队对 MyBatis 生态更熟悉
- MyBatis-Plus 提供开箱即用的 CRUD、分页、逻辑删除等功能
- 相比 JPA/Hibernate，SQL 更透明，便于优化复杂查询
- 与 Spring Boot 3.x 集成良好

## 后果

- 需要手动编写复杂 SQL（但这是可接受的）
- 不享受 JPA 的自动 DDL 生成，需自行管理数据库迁移
