/**
 * 实验室 & 开放时间类型
 */

import type { LabStatus } from "./enums";

export interface Lab {
  id: string;
  name: string;
  location: string;
  capacity: number;
  description: string | null;
  imageUrl: string | null;
  equipmentNum: number;
  status: LabStatus;
  managerId: string | null;
  managerName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LabCreateRequest {
  name: string;
  location: string;
  capacity: number;
  description?: string;
  imageUrl?: string;
  managerId?: string;
}

export interface LabUpdateRequest {
  name?: string;
  location?: string;
  capacity?: number;
  description?: string;
  imageUrl?: string;
  status?: LabStatus;
  managerId?: string;
}

export interface LabHours {
  id: string;
  labId: string;
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
}

export interface LabHoursBatchRequest {
  labId: string;
  hours: Omit<LabHours, "id" | "labId">[];
}

/** 实验室列表查询筛选 */
export interface LabQuery extends Record<string, unknown> {
  name?: string;
  status?: LabStatus;
  cursor?: string;
  limit?: number;
}
