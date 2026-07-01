import client from "./client";
import type {
  DashboardKpi,
  LabUsageStat,
  EquipmentUsageStat,
  StudentRanking,
} from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export function getDashboardKpi(): Promise<DashboardKpi> {
  return client
    .get("/dashboard/kpi")
    .then((res: AxiosResponse<ApiResponse<DashboardKpi>>) => res.data.data);
}

export function getLabUsage(params?: {
  dateFrom?: string;
  dateTo?: string;
}): Promise<LabUsageStat[]> {
  return client
    .get("/dashboard/lab-usage", { params })
    .then((res: AxiosResponse<ApiResponse<LabUsageStat[]>>) => res.data.data);
}

export function getEquipmentUsage(params?: {
  dateFrom?: string;
  dateTo?: string;
}): Promise<EquipmentUsageStat[]> {
  return client
    .get("/dashboard/equipment-usage", { params })
    .then((res: AxiosResponse<ApiResponse<EquipmentUsageStat[]>>) => res.data.data);
}

export function getStudentRanking(params?: {
  dateFrom?: string;
  dateTo?: string;
  limit?: number;
}): Promise<StudentRanking[]> {
  return client
    .get("/dashboard/student-ranking", { params })
    .then((res: AxiosResponse<ApiResponse<StudentRanking[]>>) => res.data.data);
}
