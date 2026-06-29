/**
 * 用户相关类型
 */

import type { UserRole } from "./enums";

export interface User {
  id: string;
  username: string;
  realName: string;
  email: string | null;
  phone: string | null;
  role: UserRole;
  avatar: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateRequest {
  username: string;
  password: string;
  realName: string;
  role: UserRole;
  email?: string;
  phone?: string;
}

export interface UserUpdateRequest {
  realName?: string;
  email?: string;
  phone?: string;
  role?: UserRole;
  enabled?: boolean;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}
