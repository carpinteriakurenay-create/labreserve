/**
 * 设备 & 借用相关类型
 *
 * 对应后端：com.labreserve.controller.EquipmentController, BorrowController
 * @packageDocumentation
 */

import type { BorrowStatus, EquipmentStatus } from "./enums";

// ── 设备 ──

/**
 * 设备信息
 *
 * 对应后端 DTO：com.labreserve.dto.EquipmentVO
 */
export interface Equipment {
  /** 设备 ID */
  id: string;
  /** 所属实验室 ID */
  labId: string;
  /** 实验室名称（后端 JOIN 填充） */
  labName?: string;
  /** 设备名称 */
  name: string;
  /** 设备型号 */
  model: string | null;
  /** 序列号（唯一约束） */
  serialNumber: string;
  /** 详细描述 */
  description: string | null;
  /** 当前状态 */
  status: EquipmentStatus;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 创建/登记设备请求
 *
 * 需要角色：ADMIN
 */
export interface EquipmentCreateRequest {
  /** 所属实验室 ID */
  labId: string;
  /** 设备名称 */
  name: string;
  /** 设备型号（可选） */
  model?: string;
  /** 序列号（必填，全局唯一） */
  serialNumber: string;
  /** 描述（可选） */
  description?: string;
}

/**
 * 更新设备请求
 *
 * 需要角色：ADMIN，所有字段可选
 */
export interface EquipmentUpdateRequest {
  /** 新实验室 ID */
  labId?: string;
  /** 新名称 */
  name?: string;
  /** 新型号 */
  model?: string;
  /** 新序列号 */
  serialNumber?: string;
  /** 新描述 */
  description?: string;
  /** 新状态（用于 PUT /api/equipment/{id}/status） */
  status?: EquipmentStatus;
}

/**
 * 设备列表查询参数
 */
export interface EquipmentQuery {
  /** 按实验室筛选 */
  labId?: string;
  /** 按状态筛选 */
  status?: EquipmentStatus;
  /** 按名称模糊搜索 */
  name?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}

// ── 借用 ──

/**
 * 设备借用记录
 *
 * 对应后端 DTO：com.labreserve.dto.BorrowVO
 */
export interface Borrow {
  /** 借用记录 ID */
  id: string;
  /** 设备 ID */
  equipmentId: string;
  /** 设备名称（后端 JOIN 填充） */
  equipmentName?: string;
  /** 借用人用户 ID */
  userId: string;
  /** 借用人用户名 */
  userName?: string;
  /** 借用人真实姓名（后端 JOIN 填充） */
  userRealName?: string;
  /** 借用日期，格式 yyyy-MM-dd */
  borrowDate: string;
  /** 预计归还日期，格式 yyyy-MM-dd */
  expectedReturn: string;
  /** 实际归还日期（管理员确认后填写），格式 yyyy-MM-dd */
  actualReturn: string | null;
  /** 借用用途 */
  purpose: string;
  /** 借用状态 */
  status: BorrowStatus;
  /** 驳回理由（status=REJECTED 时有值） */
  rejectReason: string | null;
  /** 审批人 ID */
  approverId: string | null;
  /** 审批人姓名 */
  approverName?: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 借用申请请求
 *
 * 对应后端 DTO：com.labreserve.dto.BorrowCreateRequest
 * 使用角色：STUDENT/TEACHER/ADMIN
 * 设备必须处于 AVAILABLE 状态
 */
export interface BorrowCreateRequest {
  /** 目标设备 ID */
  equipmentId: string;
  /** 借用日期，格式 yyyy-MM-dd */
  borrowDate: string;
  /** 预计归还日期，格式 yyyy-MM-dd，必须晚于 borrowDate */
  expectedReturn: string;
  /** 借用用途（必填） */
  purpose: string;
}

/**
 * 借用列表查询参数
 */
export interface BorrowQuery {
  /** 按状态筛选 */
  status?: BorrowStatus;
  /** 按设备筛选 */
  equipmentId?: string;
  /** 按借用人筛选 */
  userId?: string;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
