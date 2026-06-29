/**
 * 通用 API 类型 — LabReserve
 */

// ── 分页（cursor-based） ──

/** 游标分页请求参数 */
export interface CursorPageQuery {
  /** 上一页最后一条记录的游标（首次请求不传） */
  cursor?: string;
  /** 每页条数，默认 20，最大 100 */
  limit?: number;
  /** 排序字段 */
  sortBy?: string;
  /** 排序方向 */
  sortOrder?: "asc" | "desc";
}

/** 游标分页响应 */
export interface CursorPageResult<T> {
  records: T[];
  /** 本次返回条数 */
  count: number;
  /** 是否有下一页 */
  hasMore: boolean;
  /** 下一页游标（本页最后一条记录的 ID） */
  nextCursor: string | null;
  /** 总记录数（可选，统计场景使用） */
  total?: number;
}

// ── API 响应 ──

/** 统一错误响应 */
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

/** 统一成功响应 */
export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
}

/** 标准成功响应 code */
export const SUCCESS_CODE = "SUCCESS";

// ── 认证 ──

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  realName: string;
  email?: string;
  phone?: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
  user: UserInfo;
}

export interface UserInfo {
  id: string;
  username: string;
  realName: string;
  role: UserRole;
  avatar: string | null;
  email: string | null;
  phone: string | null;
  enabled: boolean;
}

import type { UserRole } from "./enums";
