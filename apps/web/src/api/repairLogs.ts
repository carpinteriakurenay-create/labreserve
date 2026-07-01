import client from "./client";
import type { RepairLog, RepairLogCreateRequest, ApiResponse } from "@labreserve/shared";
import type { AxiosResponse } from "axios";

export interface RepairLogPage {
  records: RepairLog[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface RepairLogQueryParams {
  equipmentId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getRepairLogs(params: RepairLogQueryParams): Promise<RepairLogPage> {
  return client
    .get("/repair-logs", { params })
    .then((res: AxiosResponse<ApiResponse<RepairLogPage>>) => res.data.data);
}

export function getRepairLogById(id: number): Promise<RepairLog> {
  return client
    .get(`/repair-logs/${id}`)
    .then((res: AxiosResponse<ApiResponse<RepairLog>>) => res.data.data);
}

export function createRepairLog(data: RepairLogCreateRequest): Promise<RepairLog> {
  return client
    .post("/repair-logs", data)
    .then((res: AxiosResponse<ApiResponse<RepairLog>>) => res.data.data);
}

export function updateRepairLog(
  id: number,
  data: { description?: string; status?: string },
): Promise<RepairLog> {
  return client
    .put(`/repair-logs/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<RepairLog>>) => res.data.data);
}

export function updateRepairLogStatus(id: number, status: string): Promise<RepairLog> {
  return client
    .put(`/repair-logs/${id}/status`, { status })
    .then((res: AxiosResponse<ApiResponse<RepairLog>>) => res.data.data);
}
