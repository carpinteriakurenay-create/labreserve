/**
 * 预约相关类型
 *
 * 对应后端：com.labreserve.controller.BookingController
 * @packageDocumentation
 */

import type { BookingStatus } from "./enums";

/**
 * 预约记录
 *
 * 对应后端 DTO：com.labreserve.dto.BookingVO
 */
export interface Booking {
  /** 预约 ID */
  id: string;
  /** 实验室 ID */
  labId: string;
  /** 实验室名称（后端 JOIN 填充） */
  labName?: string;
  /** 预约人用户 ID */
  userId: string;
  /** 预约人用户名 */
  userName?: string;
  /** 预约人真实姓名（后端 JOIN 填充） */
  userRealName?: string;
  /** 预约日期，格式 yyyy-MM-dd */
  date: string;
  /** 开始时间，格式 HH:mm */
  startTime: string;
  /** 结束时间，格式 HH:mm */
  endTime: string;
  /** 预约用途说明 */
  purpose: string;
  /** 参与人数 */
  personCount: number;
  /** 预约状态 */
  status: BookingStatus;
  /** 驳回理由（status=REJECTED 时有值） */
  rejectReason: string | null;
  /** 审批人 ID */
  approverId: string | null;
  /** 审批人姓名（后端 JOIN 填充） */
  approverName?: string;
  /** 审批时间（ISO 格式） */
  approvedAt: string | null;
  /** 完成时间（ISO 格式） */
  completedAt: string | null;
  /** 创建时间（ISO 格式） */
  createdAt: string;
  /** 更新时间（ISO 格式） */
  updatedAt: string;
}

/**
 * 创建预约请求
 *
 * 对应后端 DTO：com.labreserve.dto.BookingCreateRequest
 * 需要认证，角色：STUDENT/TEACHER/ADMIN
 */
export interface BookingCreateRequest {
  /** 目标实验室 ID */
  labId: string;
  /** 预约日期，格式 yyyy-MM-dd */
  date: string;
  /** 开始时间，格式 HH:mm，必须在实验室开放时间内 */
  startTime: string;
  /** 结束时间，格式 HH:mm，必须晚于 startTime */
  endTime: string;
  /** 预约用途（必填，最长 500 字符） */
  purpose: string;
  /** 参与人数（默认 1） */
  personCount?: number;
}

/**
 * 修改预约请求
 *
 * 对应后端 DTO：com.labreserve.dto.BookingUpdateRequest
 * 仅 PENDING 状态的预约可修改
 */
export interface BookingUpdateRequest {
  /** 新日期（可选） */
  date?: string;
  /** 新开始时间（可选） */
  startTime?: string;
  /** 新结束时间（可选） */
  endTime?: string;
  /** 新用途（可选） */
  purpose?: string;
  /** 新参与人数（可选） */
  personCount?: number;
}

/**
 * 审批操作请求
 *
 * 对应后端 DTO：com.labreserve.dto.ApprovalRequest
 * 需要角色：TEACHER/ADMIN
 */
export interface ApprovalRequest {
  /** true=通过，false=驳回 */
  approved: boolean;
  /** 驳回理由（approved=false 时必填） */
  rejectReason?: string;
}

/**
 * 可用时间段查询参数
 *
 * 用于查询指定实验室在某天的时段可用性。
 * GET /api/bookings/available-slots
 */
export interface AvailableSlotQuery {
  /** 实验室 ID */
  labId: string;
  /** 日期，格式 yyyy-MM-dd */
  date: string;
}

/**
 * 时段可用性
 *
 * 表示一个小时的时间段是否可预约。
 */
export interface TimeSlot {
  /** 开始时间，格式 HH:mm */
  startTime: string;
  /** 结束时间，格式 HH:mm */
  endTime: string;
  /** 该时段是否可预约 */
  available: boolean;
}

/**
 * 预约列表查询参数
 *
 * GET /api/bookings
 * 学生只能查看自己的预约，教师/管理员可查看全部。
 */
export interface BookingQuery {
  /** 按状态筛选 */
  status?: BookingStatus;
  /** 按实验室筛选 */
  labId?: string;
  /** 按申请人筛选（教师/管理员可用） */
  userId?: string;
  /** 日期范围起始，格式 yyyy-MM-dd */
  dateFrom?: string;
  /** 日期范围截止，格式 yyyy-MM-dd */
  dateTo?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
