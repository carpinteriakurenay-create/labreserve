/**
 * 用户相关类型
 *
 * 对应后端：com.labreserve.controller.UserController, AuthController
 * @packageDocumentation
 */

import type { UserRole } from "./enums";

/**
 * 用户信息（管理列表使用）
 *
 * 对应后端 DTO：com.labreserve.dto.UserVO
 */
export interface User {
  /** 用户 ID */
  id: string;
  /** 用户名（学号/工号） */
  username: string;
  /** 真实姓名 */
  realName: string;
  /** 邮箱 */
  email: string | null;
  /** 手机号 */
  phone: string | null;
  /** 角色 */
  role: UserRole;
  /** 头像 URL */
  avatar: string | null;
  /** 账号是否启用 */
  enabled: boolean;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/**
 * 创建用户请求（ADMIN 专属）
 *
 * 对应后端 DTO：com.labreserve.dto.UserCreateRequest
 */
export interface UserCreateRequest {
  /** 用户名（必填，唯一） */
  username: string;
  /** 密码（必填，最少 6 位） */
  password: string;
  /** 真实姓名（必填） */
  realName: string;
  /** 角色（可指定任意角色） */
  role: UserRole;
  /** 邮箱（可选） */
  email?: string;
  /** 手机号（可选） */
  phone?: string;
}

/**
 * 更新用户请求（ADMIN 专属）
 *
 * 对应后端 DTO：com.labreserve.dto.UserUpdateRequest
 * 所有字段可选，仅更新传入的字段。
 */
export interface UserUpdateRequest {
  /** 新姓名 */
  realName?: string;
  /** 新邮箱 */
  email?: string;
  /** 新手机号 */
  phone?: string;
  /** 新角色 */
  role?: UserRole;
  /** 启用/禁用（通过 PUT /api/users/{id}/toggle-enabled） */
  enabled?: boolean;
}

/**
 * 修改密码请求
 *
 * 需要验证原密码。仅可修改自己的密码。
 * 对应后端 DTO：com.labreserve.dto.ChangePasswordRequest
 */
export interface ChangePasswordRequest {
  /** 原密码（需验证） */
  oldPassword: string;
  /** 新密码（最少 6 位） */
  newPassword: string;
}
