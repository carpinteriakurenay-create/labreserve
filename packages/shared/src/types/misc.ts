/**
 * 统计报表 & 杂项类型
 */

// ── 仪表盘 ──

export interface DashboardKpi {
  todayBookings: number;
  todayBorrows: number;
  labUsageRate: number;
  pendingApprovals: number;
}

export interface LabUsageStat {
  labId: string;
  labName: string;
  bookingCount: number;
  usageHours: number;
  utilizationRate: number;
}

export interface EquipmentUsageStat {
  equipmentId: string;
  equipmentName: string;
  borrowCount: number;
  avgBorrowDays: number;
}

export interface StudentRanking {
  userId: string;
  userRealName: string;
  bookingCount: number;
  totalHours: number;
}

// ── 使用记录 ──

export interface UsageRecord {
  id: string;
  bookingId: string;
  labId: string;
  labName: string;
  userId: string;
  userRealName: string;
  date: string;
  startTime: string;
  endTime: string;
  purpose: string;
  personCount: number;
  completedAt: string;
}

export interface UsageRecordQuery {
  labId?: string;
  userId?: string;
  dateFrom?: string;
  dateTo?: string;
  cursor?: string;
  limit?: number;
}

// ── 学生信息（管理员录入） ──

export interface Student {
  id: string;
  labId: string;
  labName?: string;
  name: string;
  gender: "MALE" | "FEMALE" | null;
  age: number | null;
  address: string | null;
  creatorId: string;
  creatorName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface StudentCreateRequest {
  labId: string;
  name: string;
  gender?: "MALE" | "FEMALE";
  age?: number;
  address?: string;
}

export interface StudentUpdateRequest {
  labId?: string;
  name?: string;
  gender?: "MALE" | "FEMALE";
  age?: number;
  address?: string;
}

export interface StudentQuery {
  labId?: string;
  name?: string;
  cursor?: string;
  limit?: number;
}

// ── 报修记录 ──

import type { RepairStatus } from "./enums";

export interface RepairLog {
  id: string;
  equipmentId: string;
  equipmentName?: string;
  reporterId: string;
  reporterName?: string;
  description: string;
  status: RepairStatus;
  createdAt: string;
  updatedAt: string;
}

export interface RepairLogCreateRequest {
  equipmentId: string;
  description: string;
}

export interface RepairLogQuery {
  equipmentId?: string;
  status?: RepairStatus;
  cursor?: string;
  limit?: number;
}

// ── 时间范围参数 ──

export interface DateRangeQuery {
  dateFrom?: string;
  dateTo?: string;
}

// ── 数据导出 ──

export interface ExportRequest {
  resource: "bookings" | "borrows" | "usageRecords";
  format: "csv";
  filters?: Record<string, unknown>;
}
