import client from "./client";
import type {
  Lab,
  LabCreateRequest,
  LabUpdateRequest,
  LabHours,
  LabHoursBatchRequest,
} from "@labreserve/shared";
import type { AxiosResponse } from "axios";
import type { ApiResponse } from "@labreserve/shared";

export interface LabPage {
  records: Lab[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface LabQueryParams {
  name?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export function getLabs(params: LabQueryParams): Promise<LabPage> {
  return client
    .get("/labs", { params })
    .then((res: AxiosResponse<ApiResponse<LabPage>>) => res.data.data);
}

export function createLab(data: LabCreateRequest): Promise<Lab> {
  return client.post("/labs", data).then((res: AxiosResponse<ApiResponse<Lab>>) => res.data.data);
}

export function getLabById(id: number): Promise<Lab> {
  return client.get(`/labs/${id}`).then((res: AxiosResponse<ApiResponse<Lab>>) => res.data.data);
}

export function updateLab(id: number, data: LabUpdateRequest): Promise<Lab> {
  return client
    .put(`/labs/${id}`, data)
    .then((res: AxiosResponse<ApiResponse<Lab>>) => res.data.data);
}

export function deleteLab(id: number): Promise<void> {
  return client
    .delete(`/labs/${id}`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}

export function toggleLabStatus(id: number): Promise<void> {
  return client
    .put(`/labs/${id}/status`)
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}

export function getLabHours(id: number): Promise<LabHours[]> {
  return client
    .get(`/labs/${id}/hours`)
    .then((res: AxiosResponse<ApiResponse<LabHours[]>>) => res.data.data);
}

export function replaceLabHours(id: number, hours: LabHoursBatchRequest["hours"]): Promise<void> {
  return client
    .put(`/labs/${id}/hours`, { hours })
    .then((res: AxiosResponse<ApiResponse<void>>) => res.data.data);
}
