/**
 * 实验室 & 开放时间类型
 *
 * 对应后端：com.labreserve.controller.LabController
 * @packageDocumentation
 */

import type { LabStatus } from "./enums";

/**
 * 实验室信息
 *
 * 对应后端 DTO：com.labreserve.dto.LabVO
 */
export interface Lab {
  /** 实验室 ID */
  id: string;
  /** 实验室名称 */
  name: string;
  /** 地理位置（如 "教学楼A-301"） */
  location: string;
  /** 最大容量（人数） */
  capacity: number;
  /** 详细描述 */
  description: string | null;
  /** 图片 URL */
  imageUrl: string | null;
  /** 设备数量 */
  equipmentNum: number;
  /** 当前状态 */
  status: LabStatus;
  /** 管理员 ID */
  managerId: string | null;
  /** 管理员姓名（后端 JOIN 填充） */
  managerName?: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 创建实验室请求
 *
 * 对应后端 DTO：com.labreserve.dto.LabCreateRequest
 * 需要角色：ADMIN
 */
export interface LabCreateRequest {
  /** 名称（必填） */
  name: string;
  /** 位置（必填） */
  location: string;
  /** 容量 */
  capacity: number;
  /** 描述 */
  description?: string;
  /** 图片 URL */
  imageUrl?: string;
  /** 管理员 ID */
  managerId?: string;
}

/**
 * 更新实验室请求
 *
 * 对应后端 DTO：com.labreserve.dto.LabUpdateRequest
 * 需要角色：ADMIN，所有字段可选（仅更新传入的字段）
 */
export interface LabUpdateRequest {
  /** 新名称 */
  name?: string;
  /** 新位置 */
  location?: string;
  /** 新容量 */
  capacity?: number;
  /** 新描述 */
  description?: string;
  /** 新图片 URL */
  imageUrl?: string;
  /** 新状态（用于 PUT /api/labs/{id}/status） */
  status?: LabStatus;
  /** 新管理员 */
  managerId?: string;
}

/**
 * 实验室开放时间
 *
 * 对应后端 DTO：com.labreserve.dto.LabHoursVO
 */
export interface LabHours {
  /** 记录 ID */
  id: string;
  /** 实验室 ID */
  labId: string;
  /** 星期几（1=周一，7=周日） */
  dayOfWeek: number;
  /** 开放时间，格式 HH:mm */
  openTime: string;
  /** 关闭时间，格式 HH:mm */
  closeTime: string;
}

/**
 * 批量设置开放时间请求
 *
 * 对应后端 DTO：com.labreserve.dto.LabHoursBatchRequest
 * 全量替换：先删除所有旧记录，再插入新记录。
 * 需要角色：ADMIN
 */
export interface LabHoursBatchRequest {
  /** 实验室 ID */
  labId: string;
  /** 开放时段列表 */
  hours: Omit<LabHours, "id" | "labId">[];
}

/**
 * 实验室列表查询参数
 *
 * GET /api/labs
 * 所有认证角色均可访问。
 */
export interface LabQuery extends Record<string, unknown> {
  /** 按名称模糊搜索 */
  name?: string;
  /** 按状态筛选 */
  status?: LabStatus;
  /** 分页游标 */
  cursor?: string;
  /** 每页条数 */
  limit?: number;
}
