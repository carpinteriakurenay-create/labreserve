/**
 * 设备 & 借用相关类型
 */

import type { BorrowStatus, EquipmentStatus } from "./enums";

// ── 设备 ──

export interface Equipment {
  id: string;
  labId: string;
  labName?: string;
  name: string;
  model: string | null;
  serialNumber: string;
  description: string | null;
  status: EquipmentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentCreateRequest {
  labId: string;
  name: string;
  model?: string;
  serialNumber: string;
  description?: string;
}

export interface EquipmentUpdateRequest {
  labId?: string;
  name?: string;
  model?: string;
  serialNumber?: string;
  description?: string;
  status?: EquipmentStatus;
}

export interface EquipmentQuery {
  labId?: string;
  status?: EquipmentStatus;
  name?: string;
  cursor?: string;
  limit?: number;
}

// ── 借用 ──

export interface Borrow {
  id: string;
  equipmentId: string;
  equipmentName?: string;
  userId: string;
  userName?: string;
  userRealName?: string;
  borrowDate: string;
  expectedReturn: string;
  actualReturn: string | null;
  purpose: string;
  status: BorrowStatus;
  rejectReason: string | null;
  approverId: string | null;
  approverName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BorrowCreateRequest {
  equipmentId: string;
  borrowDate: string;
  expectedReturn: string;
  purpose: string;
}

export interface BorrowQuery {
  status?: BorrowStatus;
  equipmentId?: string;
  userId?: string;
  cursor?: string;
  limit?: number;
}
