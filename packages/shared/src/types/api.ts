/**
 * 通用 API 类型 — LabReserve
 *
 * 定义前后端共享的请求/响应类型、分页结构、认证接口。
 * 所有类型与后端 com.labreserve.dto 包中的 DTO 一一对应。
 *
 * @packageDocumentation
 */

// ── 分页（cursor-based） ──

/**
 * 游标分页请求参数
 *
 * 使用游标而非偏移量的分页方式，在数据量大时性能优于传统 LIMIT/OFFSET。
 * 游标通常为上一页最后一条记录的 ID。
 *
 * @example
 * ```ts
 * // 首页请求
 * const firstPage: CursorPageQuery = { limit: 20 }
 * // 下一页请求（使用上次响应中的 nextCursor）
 * const nextPage: CursorPageQuery = { cursor: "42", limit: 20 }
 * ```
 */
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

/**
 * 游标分页响应
 *
 * @typeParam T - 记录类型（如 Booking、Lab 等）
 */
export interface CursorPageResult<T> {
  /** 当前页的记录列表 */
  records: T[];
  /** 本次返回条数 */
  count: number;
  /** 是否有下一页 */
  hasMore: boolean;
  /** 下一页游标（本页最后一条记录的 ID），无下一页时为 null */
  nextCursor: string | null;
  /** 总记录数（可选，统计场景使用） */
  total?: number;
}

// ── API 响应 ──

/**
 * 统一错误响应
 *
 * 当 API 返回非 SUCCESS 状态时使用此格式。
 * 后端对应：com.labreserve.dto.ApiError
 *
 * @example
 * ```json
 * { "code": "NOT_FOUND", "message": "实验室不存在", "details": { "labId": "999" } }
 * ```
 */
export interface ApiError {
  /** 错误码（如 NOT_FOUND、UNAUTHORIZED 等） */
  code: string;
  /** 人类可读的错误描述 */
  message: string;
  /** 字段级错误详情（校验错误时返回） */
  details?: Record<string, unknown>;
}

/**
 * 统一成功响应
 *
 * 所有 API 成功响应都使用此格式。
 * 后端对应：com.labreserve.dto.ApiResponse<T>
 *
 * @typeParam T - data 字段的类型
 *
 * @example
 * ```ts
 * type LabListResponse = ApiResponse<CursorPageResult<Lab>>;
 * ```
 */
export interface ApiResponse<T = unknown> {
  /** 标准成功码 "SUCCESS" */
  code: string;
  /** 操作描述消息 */
  message: string;
  /** 响应数据负载 */
  data: T;
}

/** 标准成功响应 code */
export const SUCCESS_CODE = "SUCCESS";

// ── 认证 ──

/**
 * 登录请求
 *
 * 后端验证路径：AuthController.login() → AuthService.login() →
 * AuthenticationManager.authenticate()
 */
export interface LoginRequest {
  /** 用户名（学号/工号） */
  username: string;
  /** 密码（明文，传输层由 HTTPS 保护） */
  password: string;
}

/**
 * 注册请求
 *
 * 后端验证路径：AuthController.register() → AuthService.register()
 * 默认注册角色为 STUDENT
 */
export interface RegisterRequest {
  /** 用户名（学号/工号），唯一约束 */
  username: string;
  /** 密码，最少 6 位 */
  password: string;
  /** 真实姓名 */
  realName: string;
  /** 邮箱（可选） */
  email?: string;
  /** 手机号（可选） */
  phone?: string;
}

/**
 * 登录响应
 *
 * 包含 JWT token、过期时间和用户基本信息。
 */
export interface LoginResponse {
  /** JWT Bearer token */
  token: string;
  /** token 有效期（秒），默认 86400（24h） */
  expiresIn: number;
  /** 当前登录用户信息 */
  user: UserInfo;
}

/**
 * 当前用户信息
 *
 * 用于 GET /api/auth/me 响应和 LoginResponse 中的 user 字段。
 */
export interface UserInfo {
  /** 用户 ID */
  id: string;
  /** 用户名 */
  username: string;
  /** 真实姓名 */
  realName: string;
  /** 角色：STUDENT | TEACHER | ADMIN */
  role: UserRole;
  /** 头像 URL（可选） */
  avatar: string | null;
  /** 邮箱（可选） */
  email: string | null;
  /** 手机号（可选） */
  phone: string | null;
  /** 账号是否启用 */
  enabled: boolean;
}

import type { UserRole } from "./enums";
