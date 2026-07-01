import client from "./client";
import type { UsageRecord } from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface UsageRecordPage {
  records: UsageRecord[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface UsageRecordQueryParams {
  labId?: number;
  userId?: number;
  dateFrom?: string;
  dateTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getUsageRecords(params: UsageRecordQueryParams): Promise<UsageRecordPage> {
  return client
    .get("/usage-records", { params })
    .then((res: AxiosResponse<ApiResponse<UsageRecordPage>>) => res.data.data);
}

export function exportUsageRecordsCsv(
  params: Omit<UsageRecordQueryParams, "pageNum" | "pageSize">,
): Promise<Blob> {
  return client
    .get("/usage-records/export", { params, responseType: "blob" })
    .then((res: AxiosResponse<Blob>) => res.data);
}
