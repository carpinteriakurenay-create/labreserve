/**
 * 预约相关类型
 */

import type { BookingStatus } from "./enums";

export interface Booking {
  id: string;
  labId: string;
  labName?: string;
  userId: string;
  userName?: string;
  userRealName?: string;
  date: string;
  startTime: string;
  endTime: string;
  purpose: string;
  personCount: number;
  status: BookingStatus;
  rejectReason: string | null;
  approverId: string | null;
  approverName?: string;
  approvedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookingCreateRequest {
  labId: string;
  date: string;
  startTime: string;
  endTime: string;
  purpose: string;
  personCount?: number;
}

export interface BookingUpdateRequest {
  date?: string;
  startTime?: string;
  endTime?: string;
  purpose?: string;
  personCount?: number;
}

/** 审批操作 */
export interface ApprovalRequest {
  approved: boolean;
  rejectReason?: string;
}

/** 可用时间段查询 */
export interface AvailableSlotQuery {
  labId: string;
  date: string;
}

export interface TimeSlot {
  startTime: string;
  endTime: string;
  available: boolean;
}

/** 预约列表查询筛选 */
export interface BookingQuery {
  status?: BookingStatus;
  labId?: string;
  userId?: string;
  dateFrom?: string;
  dateTo?: string;
  cursor?: string;
  limit?: number;
}
