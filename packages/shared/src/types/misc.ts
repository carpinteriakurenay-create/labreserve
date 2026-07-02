/**
 * 统计报表 & 杂项类型
 *
 * 对应后端：DashboardController, UsageRecordController, StudentController, RepairLogController
 * @packageDocumentation
 */

// ── 仪表盘 ──

/**
 * 仪表盘综合 KPI
 *
 * 对应后端 DTO：com.labreserve.dto.DashboardKpiVO
 * 需要角色：STUDENT（仅看自己的）/ TEACHER / ADMIN
 */
export interface DashboardKpi {
  /** 今日已完成预约数 */
  todayBookings: number;
  /** 今日借用申请数 */
  todayBorrows: number;
  /** 实验室使用率（0.0 - 1.0，有预约的实验室 / 总实验室） */
  labUsageRate: number;
  /** 待审批预约数（教师/管理员为全部，学生为自己的） */
  pendingApprovals: number;
}

/**
 * 实验室使用统计
 *
 * 对应后端 DTO：com.labreserve.dto.LabUsageStatVO
 * 缓存 TTL：5 分钟
 */
export interface LabUsageStat {
  /** 实验室 ID */
  labId: string;
  /** 实验室名称 */
  labName: string;
  /** 指定时间段内预约次数 */
  bookingCount: number;
  /** 累计使用小时数 */
  usageHours: number;
  /** 利用率（预留字段） */
  utilizationRate: number;
}

/**
 * 设备利用率统计
 *
 * 对应后端 DTO：com.labreserve.dto.EquipmentUsageStatVO
 * 缓存 TTL：5 分钟
 */
export interface EquipmentUsageStat {
  /** 设备 ID */
  equipmentId: string;
  /** 设备名称 */
  equipmentName: string;
  /** 借用次数 */
  borrowCount: number;
  /** 平均借用天数 */
  avgBorrowDays: number;
}

/**
 * 学生使用排行
 *
 * 对应后端 DTO：com.labreserve.dto.StudentRankingVO
 * 需要角色：TEACHER/ADMIN
 * 缓存 TTL：5 分钟
 */
export interface StudentRanking {
  /** 用户 ID */
  userId: string;
  /** 用户姓名 */
  userRealName: string;
  /** 预约次数（降序排列） */
  bookingCount: number;
  /** 累计使用小时数 */
  totalHours: number;
}

// ── 使用记录 ──

/**
 * 使用记录（已完成预约的归档视图）
 *
 * 对应后端 DTO：com.labreserve.dto.UsageRecordVO
 * 需要角色：TEACHER/ADMIN
 */
export interface UsageRecord {
  /** 记录 ID */
  id: string;
  /** 关联预约 ID */
  bookingId: string;
  /** 实验室 ID */
  labId: string;
  /** 实验室名称 */
  labName: string;
  /** 使用人 ID */
  userId: string;
  /** 使用人姓名 */
  userRealName: string;
  /** 使用日期 */
  date: string;
  /** 开始时间 */
  startTime: string;
  /** 结束时间 */
  endTime: string;
  /** 用途 */
  purpose: string;
  /** 人数 */
  personCount: number;
  /** 完成时间 */
  completedAt: string;
}

/** 使用记录查询参数 */
export interface UsageRecordQuery {
  /** 按实验室筛选 */
  labId?: string;
  /** 按用户筛选 */
  userId?: string;
  /** 日期范围起 */
  dateFrom?: string;
  /** 日期范围止 */
  dateTo?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}

// ── 学生信息（管理员录入） ──

/**
 * 学生/人员信息
 *
 * 对应后端 DTO：com.labreserve.dto.StudentVO
 */
export interface Student {
  /** 学生 ID */
  id: string;
  /** 关联实验室 ID */
  labId: string;
  /** 实验室名称 */
  labName?: string;
  /** 姓名 */
  name: string;
  /** 性别 */
  gender: "MALE" | "FEMALE" | null;
  /** 年龄 */
  age: number | null;
  /** 地址/备注 */
  address: string | null;
  /** 录入人 ID */
  creatorId: string;
  /** 录入人姓名 */
  creatorName?: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 录入学生请求 */
export interface StudentCreateRequest {
  /** 关联实验室 ID */
  labId: string;
  /** 姓名 */
  name: string;
  /** 性别（MALE/FEMALE） */
  gender?: "MALE" | "FEMALE";
  /** 年龄 */
  age?: number;
  /** 地址/备注 */
  address?: string;
}

/** 更新学生信息请求 */
export interface StudentUpdateRequest {
  /** 新关联实验室 */
  labId?: string;
  /** 新姓名 */
  name?: string;
  /** 新性别 */
  gender?: "MALE" | "FEMALE";
  /** 新年龄 */
  age?: number;
  /** 新地址 */
  address?: string;
}

/** 学生信息查询参数 */
export interface StudentQuery {
  /** 按实验室筛选 */
  labId?: string;
  /** 按姓名搜索 */
  name?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}

// ── 报修记录 ──

import type { RepairStatus } from "./enums";

/**
 * 设备报修记录
 *
 * 需要角色：已认证用户可提交，ADMIN 可更新状态
 */
export interface RepairLog {
  /** 报修 ID */
  id: string;
  /** 设备 ID */
  equipmentId: string;
  /** 设备名称 */
  equipmentName?: string;
  /** 报修人 ID */
  reporterId: string;
  /** 报修人姓名 */
  reporterName?: string;
  /** 故障描述 */
  description: string;
  /** 报修状态 */
  status: RepairStatus;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 提交报修请求 */
export interface RepairLogCreateRequest {
  /** 故障设备 ID */
  equipmentId: string;
  /** 故障描述 */
  description: string;
}

/** 报修记录查询参数 */
export interface RepairLogQuery {
  /** 按设备筛选 */
  equipmentId?: string;
  /** 按状态筛选 */
  status?: RepairStatus;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}

// ── 时间范围参数 ──

/** 按日期范围查询的通用参数 */
export interface DateRangeQuery {
  /** 起始日期，格式 yyyy-MM-dd */
  dateFrom?: string;
  /** 截止日期，格式 yyyy-MM-dd */
  dateTo?: string;
}

// ── 数据导出 ──

/**
 * CSV 导出请求
 *
 * POST /api/usage-records/export
 * 需要角色：ADMIN
 * 返回 Content-Type: text/csv，BOM 头保证中文兼容
 */
export interface ExportRequest {
  /** 导出资源类型 */
  resource: "bookings" | "borrows" | "usageRecords";
  /** 导出格式（目前仅支持 csv） */
  format: "csv";
  /** 导出筛选条件 */
  filters?: Record<string, unknown>;
}
